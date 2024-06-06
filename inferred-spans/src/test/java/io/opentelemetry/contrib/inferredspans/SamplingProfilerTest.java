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
package io.opentelemetry.contrib.inferredspans;

import static io.opentelemetry.contrib.inferredspans.semconv.Attributes.LINK_IS_CHILD;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;
import static org.awaitility.Awaitility.await;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.opentelemetry.contrib.inferredspans.util.DisabledOnOpenJ9;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.data.SpanData;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledForJreRange;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.JRE;
import org.junit.jupiter.api.condition.OS;

// async-profiler doesn't work on Windows
@DisabledOnOs(OS.WINDOWS)
@DisabledOnOpenJ9
class SamplingProfilerTest {

  private ProfilerTestSetup setup;

  @BeforeEach
  void setup() {
    // avoids any test failure to make other tests to fail
    getProfilerTempFiles().forEach(SamplingProfilerTest::silentDeleteFile);
  }

  @AfterEach
  void tearDown() {
    if (setup != null) {
      setup.close();
      setup = null;
    }
    getProfilerTempFiles().forEach(SamplingProfilerTest::silentDeleteFile);
  }

  @Test
  void shouldLazilyCreateTempFilesAndCleanThem() throws Exception {

    List<Path> tempFiles = getProfilerTempFiles();
    assertThat(tempFiles).isEmpty();

    // temporary files should be created on-demand, and properly deleted afterwards
    setupProfiler(false);

    assertThat(setup.profiler.getProfilingSessions())
        .describedAs("profiler should not have any session when disabled")
        .isEqualTo(0);

    assertThat(getProfilerTempFiles())
        .describedAs("should not create a temp file when disabled")
        .isEmpty();

    setup.close();
    setup = null;
    setupProfiler(true);

    awaitProfilerStarted(setup.profiler);

    assertThat(getProfilerTempFiles()).describedAs("should have created two temp files").hasSize(2);

    setup.close();
    setup = null;

    assertThat(getProfilerTempFiles())
        .describedAs("should delete temp files when profiler is stopped")
        .isEmpty();
  }

  private static List<Path> getProfilerTempFiles() {
    Path tempFolder = Paths.get(System.getProperty("java.io.tmpdir"));
    try {
      return Files.list(tempFolder)
          .filter(f -> f.getFileName().toString().startsWith("apm-"))
          .sorted()
          .collect(Collectors.toList());
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  @Test
  void shouldNotDeleteProvidedFiles() throws Exception {
    // when an existing file is provided to the profiler, we should not delete it
    // unlike the temporary files that are created by profiler itself

    InferredSpansConfiguration defaultConfig;
    try (InferredSpansProcessor profiler1 =
        InferredSpansProcessor.builder().startScheduledProfiling(false).build()) {
      defaultConfig = profiler1.profiler.config;
    }

    Path tempFile1 = Files.createTempFile("apm-provided", "test.bin");
    Path tempFile2 = Files.createTempFile("apm-provided", "test.jfr");

    try (OpenTelemetrySdk sdk = OpenTelemetrySdk.builder().build()) {

      SamplingProfiler otherProfiler =
          new SamplingProfiler(
              defaultConfig,
              new FixedClock(),
              () -> sdk.getTracer("my-tracer"),
              tempFile1.toFile(),
              tempFile2.toFile());

      otherProfiler.start();
      awaitProfilerStarted(otherProfiler);
      otherProfiler.stop();
    }

    assertThat(tempFile1).exists();
    assertThat(tempFile2).exists();
  }

  @Test
  void testStartCommand() {
    setupProfiler(false);
    assertThat(setup.profiler.createStartCommand())
        .isEqualTo("start,jfr,event=wall,cstack=n,interval=5ms,filter,file=null,safemode=0");

    setup.close();
    setupProfiler(config -> config.startScheduledProfiling(false).profilerLoggingEnabled(false));
    assertThat(setup.profiler.createStartCommand())
        .isEqualTo(
            "start,jfr,event=wall,cstack=n,interval=5ms,filter,file=null,safemode=0,loglevel=none");

    setup.close();
    setupProfiler(
        config ->
            config
                .startScheduledProfiling(false)
                .profilerLoggingEnabled(false)
                .samplingInterval(Duration.ofMillis(10))
                .asyncProfilerSafeMode(14));
    assertThat(setup.profiler.createStartCommand())
        .isEqualTo(
            "start,jfr,event=wall,cstack=n,interval=10ms,filter,file=null,safemode=14,loglevel=none");
  }

  @Test
  void testProfileTransaction() throws Exception {
    setupProfiler(true);
    awaitProfilerStarted(setup.profiler);

    Tracer tracer = setup.sdk.getTracer("manual-spans");

    boolean profilingActiveOnThread;
    Span tx = tracer.spanBuilder("transaction").startSpan();
    try (Scope scope = tx.makeCurrent()) {
      // makes sure that the rest will be captured by another profiling session
      // this tests that restoring which threads to profile works
      Thread.sleep(600);
      profilingActiveOnThread = setup.profiler.isProfilingActiveOnThread(Thread.currentThread());
      aInferred(tracer);
    } finally {
      tx.end();
    }

    await()
        .pollDelay(10, TimeUnit.MILLISECONDS)
        .timeout(5000, TimeUnit.MILLISECONDS)
        .untilAsserted(() -> assertThat(setup.getSpans()).hasSizeGreaterThanOrEqualTo(6));

    assertThat(profilingActiveOnThread).isTrue();

    Optional<SpanData> txData =
        setup.getSpans().stream().filter(s -> s.getName().equals("transaction")).findAny();
    assertThat(txData).isPresent();
    assertThat(txData.get()).hasNoParent();

    Optional<SpanData> testProfileTransaction =
        setup.getSpans().stream()
            .filter(s -> s.getName().equals("SamplingProfilerTest#testProfileTransaction"))
            .findAny();
    assertThat(testProfileTransaction).isPresent();
    assertThat(testProfileTransaction.get()).hasParent(txData.get());

    Optional<SpanData> inferredSpanA =
        setup.getSpans().stream()
            .filter(s -> s.getName().equals("SamplingProfilerTest#aInferred"))
            .findAny();
    assertThat(inferredSpanA).isPresent();
    assertThat(inferredSpanA.get()).hasParent(testProfileTransaction.get());

    Optional<SpanData> explicitSpanB =
        setup.getSpans().stream().filter(s -> s.getName().equals("bExplicit")).findAny();
    assertThat(explicitSpanB).isPresent();
    assertThat(explicitSpanB.get()).hasParent(txData.get());

    assertThat(inferredSpanA.get().getLinks())
        .hasSize(1)
        .anySatisfy(
            link -> {
              assertThat(link.getAttributes()).containsEntry(LINK_IS_CHILD, true);
              SpanData expectedSpan = explicitSpanB.get();
              Assertions.assertThat(link.getSpanContext().getTraceId())
                  .isEqualTo(expectedSpan.getTraceId());
              Assertions.assertThat(link.getSpanContext().getSpanId())
                  .isEqualTo(expectedSpan.getSpanId());
            });

    Optional<SpanData> inferredSpanC =
        setup.getSpans().stream()
            .filter(s -> s.getName().equals("SamplingProfilerTest#cInferred"))
            .findAny();
    assertThat(inferredSpanC).isPresent();
    assertThat(inferredSpanC.get()).hasParent(explicitSpanB.get());

    Optional<SpanData> inferredSpanD =
        setup.getSpans().stream()
            .filter(s -> s.getName().equals("SamplingProfilerTest#dInferred"))
            .findAny();
    assertThat(inferredSpanD).isPresent();
    assertThat(inferredSpanD.get()).hasParent(inferredSpanC.get());
  }

  @Test
  @DisabledForJreRange(max = JRE.JAVA_20)
  void testVirtualThreadsExcluded() throws Exception {
    setupProfiler(true);
    awaitProfilerStarted(setup.profiler);
    Tracer tracer = setup.sdk.getTracer("manual-spans");

    AtomicReference<Boolean> profilingActive = new AtomicReference<>();
    Runnable task =
        () -> {
          Span tx = tracer.spanBuilder("transaction").startSpan();
          try (Scope scope = tx.makeCurrent()) {
            profilingActive.set(setup.profiler.isProfilingActiveOnThread(Thread.currentThread()));
          } finally {
            tx.end();
          }
        };

    Method startVirtualThread = Thread.class.getMethod("startVirtualThread", Runnable.class);
    Thread virtual = (Thread) startVirtualThread.invoke(null, task);
    virtual.join();

    assertThat(profilingActive.get()).isFalse();
  }

  @Test
  void testPostProcessingDisabled() throws Exception {
    setupProfiler(config -> config.postProcessingEnabled(false));
    awaitProfilerStarted(setup.profiler);
    Tracer tracer = setup.sdk.getTracer("manual-spans");

    Span tx = tracer.spanBuilder("transaction").startSpan();
    try (Scope scope = tx.makeCurrent()) {
      // makes sure that the rest will be captured by another profiling session
      // this tests that restoring which threads to profile works
      Thread.sleep(600);
      aInferred(tracer);
    } finally {
      tx.end();
    }

    await()
        .pollDelay(10, TimeUnit.MILLISECONDS)
        .timeout(5000, TimeUnit.MILLISECONDS)
        .untilAsserted(() -> assertThat(setup.getSpans()).hasSize(2));

    Optional<SpanData> explicitSpanB =
        setup.getSpans().stream().filter(s -> s.getName().equals("bExplicit")).findAny();
    assertThat(explicitSpanB).isPresent();
    assertThat(explicitSpanB.get()).hasParentSpanId(tx.getSpanContext().getSpanId());
  }

  private void aInferred(Tracer tracer) throws Exception {
    Span span = tracer.spanBuilder("bExplicit").startSpan();
    try (Scope spanScope = span.makeCurrent()) {
      cInferred();
    } finally {
      span.end();
    }
    Thread.sleep(50);
  }

  private void cInferred() throws Exception {
    dInferred();
    Thread.sleep(50);
  }

  private void dInferred() throws Exception {
    Thread.sleep(50);
  }

  private void setupProfiler(boolean enabled) {
    setupProfiler(config -> config.startScheduledProfiling(enabled));
  }

  private void setupProfiler(Consumer<InferredSpansProcessorBuilder> configCustomizer) {
    setup =
        ProfilerTestSetup.create(
            config -> {
              config
                  .profilingDuration(Duration.ofMillis(500))
                  .profilerInterval(Duration.ofMillis(500))
                  .samplingInterval(Duration.ofMillis(5));
              configCustomizer.accept(config);
            });
  }

  private static void awaitProfilerStarted(SamplingProfiler profiler) {
    // ensure profiler is initialized
    await()
        .pollDelay(10, TimeUnit.MILLISECONDS)
        .timeout(6000, TimeUnit.MILLISECONDS)
        .until(() -> profiler.getProfilingSessions() > 1);
  }

  private static void silentDeleteFile(Path f) {
    try {
      Files.delete(f);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }
}
