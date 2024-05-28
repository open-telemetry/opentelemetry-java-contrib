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
import java.time.Duration;
import java.util.List;

public class InferredSpansConfiguration {

  private final boolean profilerLoggingEnabled;
  private final boolean backupDiagnosticFiles;
  private final int asyncProfilerSafeMode;
  private final boolean postProcessingEnabled;
  private final Duration samplingInterval;
  private final Duration inferredSpansMinDuration;
  private final List<WildcardMatcher> includedClasses;
  private final List<WildcardMatcher> excludedClasses;
  private final Duration profilerInterval;
  private final Duration profilingDuration;
  private final String profilerLibDirectory;

  InferredSpansConfiguration(
      boolean profilerLoggingEnabled,
      boolean backupDiagnosticFiles,
      int asyncProfilerSafeMode,
      boolean postProcessingEnabled,
      Duration samplingInterval,
      Duration inferredSpansMinDuration,
      List<WildcardMatcher> includedClasses,
      List<WildcardMatcher> excludedClasses,
      Duration profilerInterval,
      Duration profilingDuration,
      String profilerLibDirectory) {
    this.profilerLoggingEnabled = profilerLoggingEnabled;
    this.backupDiagnosticFiles = backupDiagnosticFiles;
    this.asyncProfilerSafeMode = asyncProfilerSafeMode;
    this.postProcessingEnabled = postProcessingEnabled;
    this.samplingInterval = samplingInterval;
    this.inferredSpansMinDuration = inferredSpansMinDuration;
    this.includedClasses = includedClasses;
    this.excludedClasses = excludedClasses;
    this.profilerInterval = profilerInterval;
    this.profilingDuration = profilingDuration;
    this.profilerLibDirectory = profilerLibDirectory;
  }

  public static InferredSpansProcessorBuilder builder() {
    return new InferredSpansProcessorBuilder();
  }

  public boolean isProfilingLoggingEnabled() {
    return profilerLoggingEnabled;
  }

  public int getAsyncProfilerSafeMode() {
    return asyncProfilerSafeMode;
  }

  public Duration getSamplingInterval() {
    return samplingInterval;
  }

  public Duration getInferredSpansMinDuration() {
    return inferredSpansMinDuration;
  }

  public List<WildcardMatcher> getIncludedClasses() {
    return includedClasses;
  }

  public List<WildcardMatcher> getExcludedClasses() {
    return excludedClasses;
  }

  public Duration getProfilingInterval() {
    return profilerInterval;
  }

  public Duration getProfilingDuration() {
    return profilingDuration;
  }

  public boolean isNonStopProfiling() {
    return getProfilingDuration().toMillis() >= getProfilingInterval().toMillis();
  }

  public boolean isBackupDiagnosticFiles() {
    return backupDiagnosticFiles;
  }

  public String getProfilerLibDirectory() {
    return profilerLibDirectory == null || profilerLibDirectory.isEmpty()
        ? System.getProperty("java.io.tmpdir")
        : profilerLibDirectory;
  }

  public boolean isPostProcessingEnabled() {
    return postProcessingEnabled;
  }
}
