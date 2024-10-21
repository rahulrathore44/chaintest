package com.aventstack.chaintest.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Build implements ChainTestEntity {

    private long id;
    private String workspace;
    private long startedAt = System.currentTimeMillis();
    private long endedAt;
    private long durationMs;
    private ExecutionStage executionStage;
    private String testRunner;
    private String name;
    private String result = Result.PASSED.getResult();
    private RunStats runStats;
    private Set<TagStats> tagStats;
    private static final ConcurrentHashMap<String, TagStats> tagStatsMonitor = new ConcurrentHashMap<>();
    private Set<Tag> tags;
    private String gitRepository;
    private String gitBranch;
    private String gitCommitHash;
    private String gitTags;
    private String gitCommitMessage;

    public Build() { }

    public Build(final String testRunner) {
        this.testRunner = testRunner;
    }

    public void updateStats(final Test test) {
        if (null == runStats || null == tagStats) {
            synchronized (this) {
                if (null == runStats) {
                    runStats = new RunStats(id);
                }
                if (null == tagStats) {
                    tagStats = ConcurrentHashMap.newKeySet();
                }
            }
        }

        runStats.update(test);
        for (final Tag tag : test.getTags()) {
            if (!tagStatsMonitor.containsKey(tag.getName())) {
                final TagStats ts = new TagStats(id);
                ts.setName(tag.getName());
                tagStats.add(ts);
                tagStatsMonitor.put(tag.getName(), ts);
            }
            tagStatsMonitor.get(tag.getName()).update(test);
        }
    }

    public void complete(final Result result) {
        setEndedAt(System.currentTimeMillis());
        setResult(result.getResult());
    }

    public void complete() {
        complete(Result.valueOf(result));
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getWorkspace() {
        return workspace;
    }

    public void setWorkspace(String workspace) {
        this.workspace = workspace;
    }

    public long getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(long startedAt) {
        this.startedAt = startedAt;
    }

    public Long getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(Long endedAt) {
        this.endedAt = endedAt;
        setDurationMs(endedAt - startedAt);
    }

    public long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(long durationMs) {
        this.durationMs = durationMs;
    }

    public ExecutionStage getExecutionStage() {
        return executionStage;
    }

    public void setExecutionStage(ExecutionStage executionStage) {
        this.executionStage = executionStage;
    }

    public String getTestRunner() {
        return testRunner;
    }

    public void setTestRunner(String testRunner) { this.testRunner = testRunner; }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public RunStats getRunStats() {
        return runStats;
    }

    public void setRunStats(final RunStats runStats) {
        this.runStats = runStats;
    }

    public Set<TagStats> getTagStats() {
        return tagStats;
    }

    public void setTagStats(final Set<TagStats> tagStats) {
        this.tagStats = tagStats;
    }

    public void setEndedAt(long endedAt) {
        this.endedAt = endedAt;
        this.durationMs = endedAt - startedAt;
    }

    public Set<Tag> getTags() {
        return tags;
    }

    public void setTags(Set<Tag> tags) {
        this.tags = tags;
    }

    public void addTags(final List<String> tags) {
        final Stream<Tag> t = tags.stream().map(Tag::new);
        addTags(t);
    }

    public void addTags(final Set<String> tags) {
        final Stream<Tag> t = tags.stream().map(Tag::new);
        addTags(t);
    }

    public void addTags(Collection<Tag> tags) {
        if (null == this.tags) {
            this.tags = new HashSet<>();
        }
        this.tags.addAll(tags);
    }

    public void addTags(Stream<Tag> tags) {
        if (null == this.tags) {
            this.tags = new HashSet<>();
        }
        tags.forEach(this.tags::add);
    }

    public String getGitRepository() {
        return gitRepository;
    }

    public void setGitRepository(String gitRepository) {
        this.gitRepository = gitRepository;
    }

    public String getGitBranch() {
        return gitBranch;
    }

    public void setGitBranch(String gitBranch) {
        this.gitBranch = gitBranch;
    }

    public String getGitCommitHash() {
        return gitCommitHash;
    }

    public void setGitCommitHash(String gitCommitHash) {
        this.gitCommitHash = gitCommitHash;
    }

    public String getGitTags() {
        return gitTags;
    }

    public void setGitTags(String gitTags) {
        this.gitTags = gitTags;
    }

    public String getGitCommitMessage() {
        return gitCommitMessage;
    }

    public void setGitCommitMessage(String gitCommitMessage) {
        this.gitCommitMessage = gitCommitMessage;
    }
}
