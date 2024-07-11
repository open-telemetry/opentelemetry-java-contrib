/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.inferredspans.internal.util;

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
