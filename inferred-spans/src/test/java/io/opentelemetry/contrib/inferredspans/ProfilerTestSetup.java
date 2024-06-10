/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.inferredspans;

import io.opentelemetry.contrib.inferredspans.internal.SamplingProfiler;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import java.util.List;
import java.util.function.Consumer;

public class ProfilerTestSetup implements AutoCloseable {

  public OpenTelemetrySdk sdk;

  public SamplingProfiler profiler;

  public InMemorySpanExporter spanExporter;

  public ProfilerTestSetup(
      OpenTelemetrySdk sdk, InferredSpansProcessor processor, InMemorySpanExporter spanExporter) {
    this.sdk = sdk;
    this.profiler = processor.profiler;
    this.spanExporter = spanExporter;
  }

  public List<SpanData> getSpans() {
    return spanExporter.getFinishedSpanItems();
  }

  @Override
  public void close() {
    sdk.close();
  }

  public static ProfilerTestSetup create(Consumer<InferredSpansProcessorBuilder> configCustomizer) {
    InferredSpansProcessorBuilder builder = InferredSpansProcessor.builder();
    configCustomizer.accept(builder);

    InferredSpansProcessor processor = builder.build();

    InMemorySpanExporter exporter = InMemorySpanExporter.create();

    SdkTracerProvider tracerProvider =
        SdkTracerProvider.builder()
            .addSpanProcessor(processor)
            .addSpanProcessor(SimpleSpanProcessor.create(exporter))
            .build();
    processor.setTracerProvider(tracerProvider);

    OpenTelemetrySdk sdk = OpenTelemetrySdk.builder().setTracerProvider(tracerProvider).build();

    return new ProfilerTestSetup(sdk, processor, exporter);
  }

  public static SamplingProfiler extractProfilerImpl(InferredSpansProcessor processor) {
    return processor.profiler;
  }
}
