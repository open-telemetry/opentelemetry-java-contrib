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
package io.opentelemetry.contrib.inferredspans.util;

import com.google.auto.service.AutoService;
import io.opentelemetry.exporter.logging.LoggingSpanExporter;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.util.List;

@AutoService(AutoConfigurationCustomizerProvider.class)
public class AutoConfiguredDataCapture implements AutoConfigurationCustomizerProvider {

  private static final InMemorySpanExporter inMemorySpanExporter = InMemorySpanExporter.create();

  /*
   Returns the spans which have been exported by the autoconfigured global OpenTelemetry SDK.
  */
  public static List<SpanData> getSpans() {
    return inMemorySpanExporter.getFinishedSpanItems();
  }

  @Override
  public void customize(AutoConfigurationCustomizer autoConfiguration) {
    autoConfiguration.addSpanExporterCustomizer(
        (spanExporter, config) -> {
          // we piggy-back onto the autoconfigured logging exporter for now,
          // because that one uses a SimpleSpanProcessor which does not impose a batching delay
          if (spanExporter instanceof LoggingSpanExporter) {
            inMemorySpanExporter.reset();
            return SpanExporter.composite(inMemorySpanExporter, spanExporter);
          }
          return spanExporter;
        });
  }

  @Override
  public int order() {
    // There might be other autoconfigurations wrapping SpanExporters,
    // which can result in us failing to detect it
    // We avoid this by ensuring that we run first
    return Integer.MIN_VALUE;
  }
}
