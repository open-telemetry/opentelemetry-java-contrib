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

import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import java.util.List;
import java.util.function.Consumer;

public class ProfilerTestSetup implements AutoCloseable {

  OpenTelemetrySdk sdk;

  SamplingProfiler profiler;

  InMemorySpanExporter spanExporter;

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
    InferredSpansProcessorBuilder builder = InferredSpansConfiguration.builder();
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
}
