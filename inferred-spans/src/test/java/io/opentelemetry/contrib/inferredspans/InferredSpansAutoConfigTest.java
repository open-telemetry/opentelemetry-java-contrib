/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.inferredspans;

import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;
import static org.awaitility.Awaitility.await;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.opentelemetry.contrib.inferredspans.config.WildcardMatcher;
import io.opentelemetry.contrib.inferredspans.util.AutoConfigTestProperties;
import io.opentelemetry.contrib.inferredspans.util.AutoConfiguredDataCapture;
import io.opentelemetry.contrib.inferredspans.util.DisabledOnOpenJ9;
import io.opentelemetry.contrib.inferredspans.util.OtelReflectionUtils;
import io.opentelemetry.sdk.trace.SpanProcessor;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

public class InferredSpansAutoConfigTest {

  @BeforeEach
  @AfterEach
  public void resetGlobalOtel() {
    ProfilingActivationListener.ensureInitialized();
    OtelReflectionUtils.shutdownAndResetGlobalOtel();
  }

  @Test
  @DisabledOnOpenJ9
  public void checkAllOptions(@TempDir Path tmpDir) {
    String libDir = tmpDir.resolve("foo").resolve("bar").toString();
    try (AutoConfigTestProperties props =
        new AutoConfigTestProperties()
            .put(InferredSpansAutoConfig.ENABLED_OPTION, "true")
            .put(InferredSpansAutoConfig.LOGGING_OPTION, "false")
            .put(InferredSpansAutoConfig.DIAGNOSTIC_FILES_OPTION, "true")
            .put(InferredSpansAutoConfig.SAFEMODE_OPTION, "16")
            .put(InferredSpansAutoConfig.POSTPROCESSING_OPTION, "false")
            .put(InferredSpansAutoConfig.SAMPLING_INTERVAL_OPTION, "7ms")
            .put(InferredSpansAutoConfig.MIN_DURATION_OPTION, "2ms")
            .put(InferredSpansAutoConfig.INCLUDED_CLASSES_OPTION, "foo*23,bar.baz")
            .put(InferredSpansAutoConfig.EXCLUDED_CLASSES_OPTION, "blub,test*.test2")
            .put(InferredSpansAutoConfig.INTERVAL_OPTION, "2s")
            .put(InferredSpansAutoConfig.DURATION_OPTION, "3s")
            .put(InferredSpansAutoConfig.LIB_DIRECTORY_OPTION, libDir)) {

      OpenTelemetry otel = GlobalOpenTelemetry.get();
      List<SpanProcessor> processors = OtelReflectionUtils.getSpanProcessors(otel);
      assertThat(processors).filteredOn(proc -> proc instanceof InferredSpansProcessor).hasSize(1);
      InferredSpansProcessor processor =
          (InferredSpansProcessor)
              processors.stream()
                  .filter(proc -> proc instanceof InferredSpansProcessor)
                  .findFirst()
                  .get();

      InferredSpansConfiguration config = processor.profiler.config;
      assertThat(config.isProfilingLoggingEnabled()).isFalse();
      assertThat(config.isBackupDiagnosticFiles()).isTrue();
      assertThat(config.getAsyncProfilerSafeMode()).isEqualTo(16);
      assertThat(config.getSamplingInterval()).isEqualTo(Duration.ofMillis(7));
      assertThat(wildcardsAsStrings(config.getIncludedClasses()))
          .containsExactly("foo*23", "bar.baz");
      assertThat(wildcardsAsStrings(config.getExcludedClasses()))
          .containsExactly("blub", "test*.test2");
      assertThat(config.getProfilingInterval()).isEqualTo(Duration.ofSeconds(2));
      assertThat(config.getProfilingDuration()).isEqualTo(Duration.ofSeconds(3));
      assertThat(config.getProfilerLibDirectory()).isEqualTo(libDir);
    }
  }

  @Test
  public void checkDisabledbyDefault() {
    try (AutoConfigTestProperties props = new AutoConfigTestProperties()) {
      OpenTelemetry otel = GlobalOpenTelemetry.get();
      List<SpanProcessor> processors = OtelReflectionUtils.getSpanProcessors(otel);
      assertThat(processors).noneMatch(proc -> proc instanceof InferredSpansProcessor);
    }
  }

  @DisabledOnOpenJ9
  @DisabledOnOs(OS.WINDOWS)
  @Test
  public void checkProfilerWorking() {
    try (AutoConfigTestProperties props =
        new AutoConfigTestProperties()
            .put(InferredSpansAutoConfig.ENABLED_OPTION, "true")
            .put(InferredSpansAutoConfig.DURATION_OPTION, "500ms")
            .put(InferredSpansAutoConfig.INTERVAL_OPTION, "500ms")
            .put(InferredSpansAutoConfig.SAMPLING_INTERVAL_OPTION, "5ms")) {
      OpenTelemetry otel = GlobalOpenTelemetry.get();
      List<SpanProcessor> processors = OtelReflectionUtils.getSpanProcessors(otel);
      assertThat(processors).filteredOn(proc -> proc instanceof InferredSpansProcessor).hasSize(1);
      InferredSpansProcessor processor =
          (InferredSpansProcessor)
              processors.stream()
                  .filter(proc -> proc instanceof InferredSpansProcessor)
                  .findFirst()
                  .get();

      // Wait until profiler is started
      await()
          .pollDelay(Duration.ofMillis(10))
          .timeout(Duration.ofSeconds(6))
          .until(() -> processor.profiler.getProfilingSessions() > 1);

      Tracer tracer = otel.getTracer("manual-spans");

      Span tx = tracer.spanBuilder("my-root").startSpan();
      try (Scope scope = tx.makeCurrent()) {
        doSleep();
      } finally {
        tx.end();
      }

      await()
          .untilAsserted(
              () ->
                  assertThat(AutoConfiguredDataCapture.getSpans())
                      .hasSizeGreaterThanOrEqualTo(2)
                      .anySatisfy(
                          span -> {
                            assertThat(span.getName()).startsWith("InferredSpansAutoConfigTest#");
                            assertThat(span.getInstrumentationScopeInfo().getName())
                                .isEqualTo(InferredSpansProcessor.TRACER_NAME);
                            assertThat(span.getInstrumentationScopeInfo().getVersion())
                                .isNotBlank();
                          }));
    }
  }

  private static void doSleep() {
    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  private static List<String> wildcardsAsStrings(List<WildcardMatcher> wildcardList) {
    return wildcardList.stream().map(WildcardMatcher::getMatcher).collect(Collectors.toList());
  }
}
