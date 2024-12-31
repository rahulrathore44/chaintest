package com.aventstack.chaintest.service;

import com.aventstack.chaintest.conf.ConfigurationManager;
import com.aventstack.chaintest.domain.Build;
import com.aventstack.chaintest.domain.Embed;
import com.aventstack.chaintest.domain.SystemInfo;
import com.aventstack.chaintest.domain.Test;
import com.aventstack.chaintest.generator.ChainTestPropertyKeys;
import com.aventstack.chaintest.generator.Generator;
import com.aventstack.chaintest.storage.StorageService;
import com.aventstack.chaintest.storage.StorageServiceFactory;
import com.aventstack.chaintest.util.RegexUtil;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ChainPluginService {
    
    private static final Logger log = LoggerFactory.getLogger(ChainPluginService.class);
    private static final String GEN_PATTERN = "chaintest.generator.[a-zA-Z]+.enabled";
    private static final String STORAGE_SERVICE = "chaintest.storage.service";
    private static final String STORAGE_SERVICE_ENABLED = STORAGE_SERVICE + ".enabled";
    private static final List<Test> _tests = Collections.synchronizedList(new ArrayList<>());
    private static final Map<String, Embed> _embeds = new ConcurrentHashMap<>();
    private static final AtomicBoolean START_INVOKED = new AtomicBoolean();
    private static final List<String> SYS_PROPS = List.of(
            "java.version",
            "java.vm.name",
            "java.vm.vendor",
            "java.class.version",
            "java.runtime.name",
            "os.name",
            "os.arch",
            "os.version"
    );

    @Getter
    public static ChainPluginService instance;

    private final Build _build;
    private final String _testRunner;
    private final List<Generator> _generators = new ArrayList<>(3);
    private StorageService _storageService;

    public ChainPluginService(final String testRunner) {
        instance = this;
        _build = new Build(testRunner);
        _build.setSystemInfo(getProps());
        _testRunner = testRunner;
    }

    private List<SystemInfo> getProps() {
        final List<SystemInfo> list = new ArrayList<>();
        for (final String prop : SYS_PROPS) {
            list.add(new SystemInfo(prop, System.getProperty(prop)));
        }
        return list;
    }

    public Build getBuild() {
        return _build;
    }

    public void register(final Generator generator) {
        _generators.add(generator);
    }

    public void register(final Collection<Generator> generators) {
        _generators.addAll(generators);
    }

    public void start() {
        final Optional<Map<String, String>> config = Optional.ofNullable(ConfigurationManager.getConfig());
        if (!START_INVOKED.getAndSet(true)) {
            config.ifPresent(this::init);
        }
        _generators.stream().filter(x -> !x.started())
                .forEach(x -> x.start(config, _testRunner, _build));
    }

    private void init(final Map<String, String> config) {
        _build.setProjectName(config.get(ChainTestPropertyKeys.PROJECT_NAME));
        register(config);
        startStorageService(config);
    }

    private void register(final Map<String, String> config) {
        final Set<String> generatorNames = new HashSet<>();
        _generators.forEach(x -> generatorNames.add(x.getName()));
        for (final Map.Entry<String, String> entry : config.entrySet()) {
            log.trace("Reading property: {}", entry.getKey());
            if (entry.getKey().matches(GEN_PATTERN) && !generatorNames.contains(entry.getKey().split("\\.")[2])) {
                final String classNameKey = RegexUtil.match("(.*)\\.", entry.getKey()) + "class-name";
                log.debug("Found configuration entry {}, will use {} to resolve class", entry.getKey(), classNameKey);
                if (!config.containsKey(classNameKey)) {
                    log.info("{} was true from configuration but the required property {} was not provided", entry.getKey(), classNameKey);
                    continue;
                }
                final String className = config.get(classNameKey);
                try {
                    final Class<?> clazz = Class.forName(className);
                    final Generator gen = (Generator) clazz.getDeclaredConstructor().newInstance();
                    _generators.add(gen);
                } catch (Exception e) {
                    log.error("Failed to create an instance of {} generator", className, e);
                }
            }
        }
    }

    private void startStorageService(final Map<String, String> config) {
        if (config.containsKey(STORAGE_SERVICE_ENABLED) && Boolean.parseBoolean(config.get(STORAGE_SERVICE_ENABLED))) {
            final String name = config.get(STORAGE_SERVICE);
            final StorageService storageService = StorageServiceFactory.getStorageService(name);
            if (storageService.create(config)) {
                _storageService = storageService;
            }
        }
    }

    public void afterTest(final Test test, final Optional<Throwable> throwable) {
        _tests.add(test);
        _build.updateStats(test);
        _generators.forEach(x -> x.afterTest(test, throwable));
    }

    public void flush() {
        embedRemaining();
        _build.complete();
        _generators.forEach(x -> x.flush(_tests));
    }

    public void executionFinished() {
        if (null != _storageService) {
            _storageService.close();
        }
        flush();
        _generators.forEach(Generator::executionFinished);
    }

    public void embed(final String externalId, final byte[] data, final String mimeType) {
        embed(externalId, new Embed(data, mimeType));
    }

    public void embed(final String externalId, final File file, final String mimeType) {
        embed(externalId, new Embed(file, mimeType));
    }

    public void embed(final String externalId, final String base64, final String mimeType) {
        embed(externalId, new Embed(base64, mimeType));
    }

    public void embed(final String externalId, final Embed embed) {
        _embeds.put(externalId, embed);
        embed(_tests, externalId, embed);
    }

    public void embed(final Test test, final byte[] data, final String mimeType) {
        embed(test, new Embed(data, mimeType));
    }

    public void embed(final Test test, final File file, final String mimeType) {
        embed(test, new Embed(file, mimeType));
    }

    public void embed(final Test test, final String base64, final String mimeType) {
        embed(test, new Embed(base64, mimeType));
    }

    public void embed(final Test test, final Embed embed) {
        putBlob(test, embed);
        test.addEmbed(embed);
        if (null != test.getExternalId()) {
            _embeds.remove(test.getExternalId());
        }
    }

    private void putBlob(final Test test, final Embed embed) {
        if (null != _storageService) {
            _storageService.upload(test, embed);
        }
    }

    private void embed(final List<Test> tests, final String externalId, final Embed embed) {
        final Predicate<Test> predicate = test -> null != test.getExternalId() && test.getExternalId().equals(externalId);
        final Optional<Test> any = tests.stream().filter(predicate).findAny();
        if (any.isPresent()) {
            putBlob(any.get(), embed);
            embed(any.get(), embed);
            _embeds.remove(externalId);
        } else {
            tests.forEach(test -> embed(test.getChildren(), externalId, embed));
        }
    }

    private void embedRemaining() {
        for (final String externalId : _embeds.keySet()) {
            embed(_tests, externalId, _embeds.get(externalId));
        }
    }

    private List<Test> flattenTests() {
        return _tests.stream()
                .flatMap(test -> flattenTests(test).stream())
                .collect(Collectors.toList());
    }

    private List<Test> flattenTests(Test test) {
        return Stream.concat(
                Stream.of(test),
                test.getChildren().stream().flatMap(child -> flattenTests(child).stream())
        ).collect(Collectors.toList());
    }

}
