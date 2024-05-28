/*
 * Licensed to Elasticsearch B.V. under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch B.V. licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package co.elastic.otel.profiler;

import co.elastic.otel.common.config.WildcardMatcher;
import java.io.File;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Nullable;

public class InferredSpansProcessorBuilder {
  private boolean profilerLoggingEnabled = true;
  private boolean backupDiagnosticFiles = false;
  private int asyncProfilerSafeMode = 0;
  private boolean postProcessingEnabled = true;
  private Duration samplingInterval = Duration.ofMillis(50);
  private Duration inferredSpansMinDuration = Duration.ZERO;
  private List<WildcardMatcher> includedClasses = WildcardMatcher.matchAllList();
  private List<WildcardMatcher> excludedClasses =
      Arrays.asList(
          WildcardMatcher.caseSensitiveMatcher("java.*"),
          WildcardMatcher.caseSensitiveMatcher("javax.*"),
          WildcardMatcher.caseSensitiveMatcher("sun.*"),
          WildcardMatcher.caseSensitiveMatcher("com.sun.*"),
          WildcardMatcher.caseSensitiveMatcher("jdk.*"),
          WildcardMatcher.caseSensitiveMatcher("org.apache.tomcat.*"),
          WildcardMatcher.caseSensitiveMatcher("org.apache.catalina.*"),
          WildcardMatcher.caseSensitiveMatcher("org.apache.coyote.*"),
          WildcardMatcher.caseSensitiveMatcher("org.jboss.as.*"),
          WildcardMatcher.caseSensitiveMatcher("org.glassfish.*"),
          WildcardMatcher.caseSensitiveMatcher("org.eclipse.jetty.*"),
          WildcardMatcher.caseSensitiveMatcher("com.ibm.websphere.*"),
          WildcardMatcher.caseSensitiveMatcher("io.undertow.*"));
  private Duration profilerInterval = Duration.ofSeconds(5);
  private Duration profilingDuration = Duration.ofSeconds(5);
  private String profilerLibDirectory = null;

  // The following options are only intended to be modified in tests
  private SpanAnchoredClock clock = new SpanAnchoredClock();
  private boolean startScheduledProfiling = true;
  private @Nullable File activationEventsFile = null;
  private @Nullable File jfrFile = null;

  InferredSpansProcessorBuilder() {}

  public InferredSpansProcessor build() {
    InferredSpansConfiguration config =
        new InferredSpansConfiguration(
            profilerLoggingEnabled,
            backupDiagnosticFiles,
            asyncProfilerSafeMode,
            postProcessingEnabled,
            samplingInterval,
            inferredSpansMinDuration,
            includedClasses,
            excludedClasses,
            profilerInterval,
            profilingDuration,
            profilerLibDirectory);
    return new InferredSpansProcessor(
        config, clock, startScheduledProfiling, activationEventsFile, jfrFile);
  }

  /**
   * By default, async profiler prints warning messages about missing JVM symbols to standard
   * output. Set this option to {@code true} to suppress such messages
   */
  public InferredSpansProcessorBuilder profilerLoggingEnabled(boolean profilerLoggingEnabled) {
    this.profilerLoggingEnabled = profilerLoggingEnabled;
    return this;
  }

  public InferredSpansProcessorBuilder backupDiagnosticFiles(boolean backupDiagnosticFiles) {
    this.backupDiagnosticFiles = backupDiagnosticFiles;
    return this;
  }

  /**
   * Can be used for analysis: the Async Profiler's area that deals with recovering stack trace
   * frames is known to be sensitive in some systems. It is used as a bit mask using values are
   * between 0 and 31, where 0 enables all recovery attempts and 31 disables all five (corresponding
   * 1, 2, 4, 8 and 16).
   */
  public InferredSpansProcessorBuilder asyncProfilerSafeMode(int asyncProfilerSafeMode) {
    this.asyncProfilerSafeMode = asyncProfilerSafeMode;
    return this;
  }

  /**
   * Can be used to test the effect of the async-profiler in isolation from the agent's
   * post-processing.
   */
  public InferredSpansProcessorBuilder postProcessingEnabled(boolean postProcessingEnabled) {
    this.postProcessingEnabled = postProcessingEnabled;
    return this;
  }

  /**
   * The frequency at which stack traces are gathered within a profiling session. The lower you set
   * it, the more accurate the durations will be. This comes at the expense of higher overhead and
   * more spans for potentially irrelevant operations. The minimal duration of a profiling-inferred
   * span is the same as the value of this setting.
   */
  public InferredSpansProcessorBuilder samplingInterval(Duration samplingInterval) {
    this.samplingInterval = samplingInterval;
    return this;
  }

  /**
   * The minimum duration of an inferred span. Note that the min duration is also implicitly set by
   * the sampling interval. However, increasing the sampling interval also decreases the accuracy of
   * the duration of inferred spans.
   */
  public InferredSpansProcessorBuilder inferredSpansMinDuration(Duration inferredSpansMinDuration) {
    this.inferredSpansMinDuration = inferredSpansMinDuration;
    return this;
  }

  /**
   * If set, the agent will only create inferred spans for methods which match this list. Setting a
   * value may slightly reduce overhead and can reduce clutter by only creating spans for the
   * classes you are interested in. Example: org.example.myapp.*
   */
  public InferredSpansProcessorBuilder includedClasses(List<WildcardMatcher> includedClasses) {
    this.includedClasses = includedClasses;
    return this;
  }

  /** Excludes classes for which no profiler-inferred spans should be created. */
  public InferredSpansProcessorBuilder excludedClasses(List<WildcardMatcher> excludedClasses) {
    this.excludedClasses = excludedClasses;
    return this;
  }

  /** The interval at which profiling sessions should be started. */
  public InferredSpansProcessorBuilder profilerInterval(Duration profilerInterval) {
    this.profilerInterval = profilerInterval;
    return this;
  }

  /**
   * The duration of a profiling session. For sampled transactions which fall within a profiling
   * session (they start after and end before the session), so-called inferred spans will be
   * created. They appear in the trace waterfall view like regular spans. NOTE: It is not
   * recommended to set much higher durations as it may fill the activation events file and
   * async-profiler's frame buffer. Warnings will be logged if the activation events file is full.
   * If you want to have more profiling coverage, try decreasing {@link
   * #profilerInterval(Duration)}.
   */
  public InferredSpansProcessorBuilder profilingDuration(Duration profilingDuration) {
    this.profilingDuration = profilingDuration;
    return this;
  }

  public InferredSpansProcessorBuilder profilerLibDirectory(String profilerLibDirectory) {
    this.profilerLibDirectory = profilerLibDirectory;
    return this;
  }

  /** For testing only. */
  InferredSpansProcessorBuilder clock(SpanAnchoredClock clock) {
    this.clock = clock;
    return this;
  }

  /** For testing only. */
  InferredSpansProcessorBuilder startScheduledProfiling(boolean startScheduledProfiling) {
    this.startScheduledProfiling = startScheduledProfiling;
    return this;
  }

  /** For testing only. */
  InferredSpansProcessorBuilder activationEventsFile(@Nullable File activationEventsFile) {
    this.activationEventsFile = activationEventsFile;
    return this;
  }

  /** For testing only. */
  InferredSpansProcessorBuilder jfrFile(@Nullable File jfrFile) {
    this.jfrFile = jfrFile;
    return this;
  }
}
