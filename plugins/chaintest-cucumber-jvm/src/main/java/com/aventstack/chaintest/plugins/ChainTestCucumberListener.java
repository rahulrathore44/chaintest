package com.aventstack.chaintest.plugins;

import com.aventstack.chaintest.domain.Test;
import com.aventstack.chaintest.generator.ChainTestEmailGenerator;
import com.aventstack.chaintest.generator.ChainTestServiceClient;
import com.aventstack.chaintest.generator.ChainTestSimpleGenerator;
import com.aventstack.chaintest.service.ChainPluginService;
import io.cucumber.gherkin.GherkinParser;
import io.cucumber.messages.types.Envelope;
import io.cucumber.messages.types.Feature;
import io.cucumber.messages.types.GherkinDocument;
import io.cucumber.messages.types.Tag;
import io.cucumber.plugin.EventListener;
import io.cucumber.plugin.event.EmbedEvent;
import io.cucumber.plugin.event.EventHandler;
import io.cucumber.plugin.event.EventPublisher;
import io.cucumber.plugin.event.PickleStepTestStep;
import io.cucumber.plugin.event.TestCaseFinished;
import io.cucumber.plugin.event.TestCaseStarted;
import io.cucumber.plugin.event.TestRunFinished;
import io.cucumber.plugin.event.TestSourceRead;
import io.cucumber.plugin.event.TestStepFinished;
import io.cucumber.plugin.event.WriteEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

public class ChainTestCucumberListener implements EventListener {

    private static final Logger log = LoggerFactory.getLogger(ChainTestCucumberListener.class);
    private static final String CUCUMBER_JVM = "cucumber-jvm";

    private final Map<UUID, Test> _scenarios = new HashMap<>();
    private final Map<URI, Test> _features = new HashMap<>();
    private ChainPluginService _service;

    public ChainTestCucumberListener(final String ignored) throws IOException {
        _service = new ChainPluginService(CUCUMBER_JVM);
        _service.register(new ChainTestSimpleGenerator());
        //_service.register(new ChainTestServiceClient());
        _service.register(new ChainTestEmailGenerator());
        _service.start();
    }

    @Override
    public void setEventPublisher(final EventPublisher publisher) {
        publisher.registerHandlerFor(TestSourceRead.class, testSourceReadHandler);
        publisher.registerHandlerFor(TestCaseStarted.class, caseStartedHandler);
        publisher.registerHandlerFor(TestCaseFinished.class, caseFinishedHandler);
        publisher.registerHandlerFor(TestStepFinished.class, stepFinishedHandler);
        publisher.registerHandlerFor(EmbedEvent.class, embedEventhandler);
        publisher.registerHandlerFor(WriteEvent.class, writeEventhandler);
        publisher.registerHandlerFor(TestRunFinished.class, runFinishedHandler);
    }

    private final EventHandler<TestSourceRead> testSourceReadHandler = event -> {
        log.trace("Received TestSourceRead event for URI: {}", event.getUri());
        if (!_features.containsKey(event.getUri())) {
            final Optional<Feature> container = parseFeature(event);
            container.ifPresent(feature -> {
                final Stream<String> tags = feature.getTags().stream()
                        .map(Tag::getName);
                final Test test = new Test("Feature: " + feature.getName(),
                        Optional.of("Feature"),
                        tags);
                _features.put(event.getUri(), test);
            });
        }
    };

    private Optional<Feature> parseFeature(final TestSourceRead event) {
        final GherkinParser parser = GherkinParser.builder()
                .includePickles(false)
                .includeSource(false)
                .build();
        try {
            final Optional<Envelope> envelope = parser.parse(Paths.get(event.getUri().getPath()))
                    .findAny();
            if (envelope.isEmpty() || envelope.get().getGherkinDocument().isEmpty()) {
                log.error("No features were found in {}", event.getUri());
                return Optional.empty();
            }
            final GherkinDocument document = envelope.get().getGherkinDocument().get();
            if (document.getFeature().isEmpty()) {
                log.error("Feature file {} does not contain a Feature", event.getUri());
            }
            return document.getFeature();
        } catch (final IOException e) {
            log.error("Failed to load feature file {}", event.getUri(), e);
        }
        return Optional.empty();
    }

    private final EventHandler<TestCaseStarted> caseStartedHandler = event -> {
        log.debug("Scenario start: {}", event.getTestCase().getName());
        final Test scenario = new Test(event.getTestCase().getKeyword() + ": " + event.getTestCase().getName(),
                Optional.of("Scenario"),
                event.getTestCase().getTags());
        _features.get(event.getTestCase().getUri()).addChild(scenario);
        _scenarios.put(event.getTestCase().getId(), scenario);
    };

    private final EventHandler<TestCaseFinished> caseFinishedHandler = event -> {
        log.debug("Scenario end: {}", event.getTestCase().getName());
        final Test scenario = _scenarios.get(event.getTestCase().getId());
        scenario.setResult(event.getResult().getStatus().name());
        scenario.complete(event.getResult().getError());
    };

    private final EventHandler<TestStepFinished> stepFinishedHandler = event -> {
        log.debug("Step: {}", event.getTestCase().getName());
        final PickleStepTestStep pickle = ((PickleStepTestStep) event.getTestStep());
        final Test step = new Test(pickle.getStep().getKeyword() + pickle.getStep().getText(),
                Optional.of("Step"));
        _scenarios.get(event.getTestCase().getId()).addChild(step);
        step.setResult(event.getResult().getStatus().name());
        step.complete(event.getResult().getError());
    };

    private final EventHandler<EmbedEvent> embedEventhandler = event -> {
    };

    private final EventHandler<WriteEvent> writeEventhandler = event -> {
    };

    private final EventHandler<TestRunFinished> runFinishedHandler = event -> {
        _service.getBuild().setBDD(true);
        for (final Map.Entry<URI, Test> entry : _features.entrySet()) {
            log.debug("Preparing to finalize and send Feature [{}]: {}", entry.getValue().getName(), entry.getKey());
            try {
                _service.afterTest(entry.getValue(), Optional.empty());
            } catch (final Exception e) {
                log.debug("An exception occurred while sending test", e);
            }
        }
        _service.executionFinished();
    };

}
