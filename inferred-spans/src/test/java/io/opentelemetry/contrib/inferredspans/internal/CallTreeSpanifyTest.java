/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.inferredspans.internal;

import static io.opentelemetry.contrib.inferredspans.internal.semconv.Attributes.CODE_STACKTRACE;
import static io.opentelemetry.contrib.inferredspans.internal.semconv.Attributes.SPAN_IS_INFERRED;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;

import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.contrib.inferredspans.ProfilerTestSetup;
import io.opentelemetry.contrib.inferredspans.internal.pooling.ObjectPool;
import io.opentelemetry.contrib.inferredspans.internal.util.DisabledOnOpenJ9;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.OpenTelemetrySdkBuilder;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

class CallTreeSpanifyTest {

  static {
    // we can't reset context storage wrappers between tests, so we msut ensure that it is
    // registered before we create ANY Otel instance
    ProfilingActivationListener.ensureInitialized();
  }

  @Test
  @DisabledOnOs(OS.WINDOWS)
  @DisabledOnOpenJ9
  void testSpanification() throws Exception {
    FixedClock nanoClock = new FixedClock();
    try (ProfilerTestSetup setup =
        ProfilerTestSetup.create(
            config -> config.clock(nanoClock).startScheduledProfiling(false))) {
      setup.profiler.setProfilingSessionOngoing(true);
      CallTree.Root callTree =
          CallTreeTest.getCallTree(setup, new String[] {" dd   ", " cc   ", " bbb  ", "aaaaee"});
      assertThat(
              callTree.spanify(
                  nanoClock, setup.sdk.getTracer("dummy-tracer"), CallTree.DEFAULT_PARENT_OVERRIDE))
          .isEqualTo(4);
      assertThat(setup.getSpans()).hasSize(5);
      assertThat(setup.getSpans().stream().map(SpanData::getName))
          .containsExactly(
              "Call Tree Root",
              "CallTreeTest#a",
              "CallTreeTest#b",
              "CallTreeTest#d",
              "CallTreeTest#e");

      SpanData a = setup.getSpans().get(1);
      assertThat(a).hasName("CallTreeTest#a");
      assertThat(a.getEndEpochNanos() - a.getStartEpochNanos()).isEqualTo(30_000_000);
      assertThat(a.getAttributes().get(CODE_STACKTRACE)).isBlank();
      assertThat(a).hasAttribute(SPAN_IS_INFERRED, true);

      SpanData b = setup.getSpans().get(2);
      assertThat(b).hasName("CallTreeTest#b");
      assertThat(b.getEndEpochNanos() - b.getStartEpochNanos()).isEqualTo(20_000_000);
      assertThat(b.getAttributes().get(CODE_STACKTRACE)).isBlank();
      assertThat(b).hasAttribute(SPAN_IS_INFERRED, true);

      SpanData d = setup.getSpans().get(3);
      assertThat(d).hasName("CallTreeTest#d");
      assertThat(d.getEndEpochNanos() - d.getStartEpochNanos()).isEqualTo(10_000_000);
      assertThat(d.getAttributes().get(CODE_STACKTRACE))
          .isEqualTo("at " + CallTreeTest.class.getName() + ".c(CallTreeTest.java)");
      assertThat(d).hasAttribute(SPAN_IS_INFERRED, true);

      SpanData e = setup.getSpans().get(4);
      assertThat(e).hasName("CallTreeTest#e");
      assertThat(e.getEndEpochNanos() - e.getStartEpochNanos()).isEqualTo(10_000_000);
      assertThat(e.getAttributes().get(CODE_STACKTRACE)).isBlank();
      assertThat(e).hasAttribute(SPAN_IS_INFERRED, true);
    }
  }

  @Test
  void testCallTreeWithActiveSpan() {
    FixedClock nanoClock = new FixedClock();

    String traceId = "0af7651916cd43dd8448eb211c80319c";
    String rootSpanId = "b7ad6b7169203331";
    TraceContext rootContext =
        TraceContext.fromSpanContextWithZeroClockAnchor(
            SpanContext.create(
                traceId, rootSpanId, TraceFlags.getSampled(), TraceState.getDefault()),
            null);

    ObjectPool<CallTree.Root> rootPool = ObjectPool.createRecyclable(2, CallTree.Root::new);
    ObjectPool<CallTree> childPool = ObjectPool.createRecyclable(2, CallTree::new);

    CallTree.Root root = CallTree.createRoot(rootPool, rootContext.serialize(), 0);
    root.addStackTrace(Collections.singletonList(StackFrame.of("A", "a")), 0, childPool, 0);

    String childSpanId = "a1b2c3d4e5f64242";
    TraceContext spanContext =
        TraceContext.fromSpanContextWithZeroClockAnchor(
            SpanContext.create(
                traceId, childSpanId, TraceFlags.getSampled(), TraceState.getDefault()),
            rootSpanId);

    root.onActivation(spanContext.serialize(), TimeUnit.MILLISECONDS.toNanos(5));
    root.addStackTrace(
        Arrays.asList(StackFrame.of("A", "b"), StackFrame.of("A", "a")),
        TimeUnit.MILLISECONDS.toNanos(10),
        childPool,
        0);
    root.addStackTrace(
        Arrays.asList(StackFrame.of("A", "b"), StackFrame.of("A", "a")),
        TimeUnit.MILLISECONDS.toNanos(20),
        childPool,
        0);
    root.onDeactivation(
        spanContext.serialize(), rootContext.serialize(), TimeUnit.MILLISECONDS.toNanos(25));

    root.addStackTrace(
        Collections.singletonList(StackFrame.of("A", "a")),
        TimeUnit.MILLISECONDS.toNanos(30),
        childPool,
        0);
    root.end(childPool, 0);

    assertThat(root.getCount()).isEqualTo(4);
    assertThat(root.getDurationUs()).isEqualTo(30_000);
    assertThat(root.getChildren()).hasSize(1);

    CallTree a = root.getLastChild();
    assertThat(a).isNotNull();
    assertThat(a.getFrame().getMethodName()).isEqualTo("a");
    assertThat(a.getCount()).isEqualTo(4);
    assertThat(a.getDurationUs()).isEqualTo(30_000);
    assertThat(a.getChildren()).hasSize(1);

    CallTree b = a.getLastChild();
    assertThat(b).isNotNull();
    assertThat(b.getFrame().getMethodName()).isEqualTo("b");
    assertThat(b.getCount()).isEqualTo(2);
    assertThat(b.getDurationUs()).isEqualTo(10_000);
    assertThat(b.getChildren()).isEmpty();

    InMemorySpanExporter exporter = InMemorySpanExporter.create();
    OpenTelemetrySdkBuilder sdkBuilder =
        OpenTelemetrySdk.builder()
            .setTracerProvider(
                SdkTracerProvider.builder()
                    .addSpanProcessor(SimpleSpanProcessor.create(exporter))
                    .build());

    try (OpenTelemetrySdk outputSdk = sdkBuilder.build()) {
      root.spanify(
          nanoClock, outputSdk.getTracer("dummy-tracer"), CallTree.DEFAULT_PARENT_OVERRIDE);

      List<SpanData> spans = exporter.getFinishedSpanItems();
      assertThat(spans).hasSize(2);
      assertThat(spans.get(0)).hasTraceId(traceId).hasParentSpanId(rootSpanId);
      assertThat(spans.get(1)).hasTraceId(traceId).hasParentSpanId(childSpanId);
    }
  }

  @Test
  void testSpanWithInvertedActivation() {
    FixedClock nanoClock = new FixedClock();

    String traceId = "0af7651916cd43dd8448eb211c80319c";
    String rootSpanId = "77ad6b7169203331";
    TraceContext rootContext =
        TraceContext.fromSpanContextWithZeroClockAnchor(
            SpanContext.create(
                traceId, rootSpanId, TraceFlags.getSampled(), TraceState.getDefault()),
            null);

    String childSpanId = "11b2c3d4e5f64242";
    TraceContext childSpanContext =
        TraceContext.fromSpanContextWithZeroClockAnchor(
            SpanContext.create(
                traceId, childSpanId, TraceFlags.getSampled(), TraceState.getDefault()),
            rootSpanId);

    ObjectPool<CallTree.Root> rootPool = ObjectPool.createRecyclable(2, CallTree.Root::new);
    ObjectPool<CallTree> childPool = ObjectPool.createRecyclable(2, CallTree::new);

    CallTree.Root root = CallTree.createRoot(rootPool, childSpanContext.serialize(), 0);
    root.addStackTrace(Collections.singletonList(StackFrame.of("A", "a")), 10_000, childPool, 0);

    root.onActivation(rootContext.serialize(), 20_000);
    root.onDeactivation(rootContext.serialize(), childSpanContext.serialize(), 30_000);

    root.addStackTrace(Collections.singletonList(StackFrame.of("A", "a")), 40_000, childPool, 0);
    root.end(childPool, 0);

    InMemorySpanExporter exporter = InMemorySpanExporter.create();
    OpenTelemetrySdkBuilder sdkBuilder =
        OpenTelemetrySdk.builder()
            .setTracerProvider(
                SdkTracerProvider.builder()
                    .addSpanProcessor(SimpleSpanProcessor.create(exporter))
                    .build());
    try (OpenTelemetrySdk outputSdk = sdkBuilder.build()) {
      root.spanify(
          nanoClock, outputSdk.getTracer("dummy-tracer"), CallTree.DEFAULT_PARENT_OVERRIDE);

      List<SpanData> spans = exporter.getFinishedSpanItems();
      assertThat(spans).hasSize(1);
      assertThat(spans.get(0)).hasTraceId(traceId).hasParentSpanId(childSpanId);
      // the inferred span should not have any span links because this
      // span link would cause a cycle in the trace
      assertThat(spans.get(0).getLinks()).isEmpty();
    }
  }

  @Test
  void testSpanWithNestedActivation() {
    FixedClock nanoClock = new FixedClock();

    String traceId = "0af7651916cd43dd8448eb211c80319c";
    String rootSpanId = "77ad6b7169203331";
    TraceContext rootContext =
        TraceContext.fromSpanContextWithZeroClockAnchor(
            SpanContext.create(
                traceId, rootSpanId, TraceFlags.getSampled(), TraceState.getDefault()),
            null);

    ObjectPool<CallTree.Root> rootPool = ObjectPool.createRecyclable(2, CallTree.Root::new);
    ObjectPool<CallTree> childPool = ObjectPool.createRecyclable(2, CallTree::new);

    CallTree.Root root = CallTree.createRoot(rootPool, rootContext.serialize(), 0);
    root.addStackTrace(Collections.singletonList(StackFrame.of("A", "a")), 10_000, childPool, 0);

    root.onActivation(rootContext.serialize(), 20_000);
    root.onDeactivation(rootContext.serialize(), rootContext.serialize(), 30_000);

    root.addStackTrace(Collections.singletonList(StackFrame.of("A", "a")), 40_000, childPool, 0);
    root.end(childPool, 0);

    InMemorySpanExporter exporter = InMemorySpanExporter.create();
    OpenTelemetrySdkBuilder sdkBuilder =
        OpenTelemetrySdk.builder()
            .setTracerProvider(
                SdkTracerProvider.builder()
                    .addSpanProcessor(SimpleSpanProcessor.create(exporter))
                    .build());
    try (OpenTelemetrySdk outputSdk = sdkBuilder.build()) {
      root.spanify(
          nanoClock, outputSdk.getTracer("dummy-tracer"), CallTree.DEFAULT_PARENT_OVERRIDE);

      List<SpanData> spans = exporter.getFinishedSpanItems();
      assertThat(spans).hasSize(1);
      assertThat(spans.get(0)).hasTraceId(traceId).hasParentSpanId(rootSpanId);
      // the inferred span should not have any span links because this
      // span link would cause a cycle in the trace
      assertThat(spans.get(0).getLinks()).isEmpty();
    }
  }
}
