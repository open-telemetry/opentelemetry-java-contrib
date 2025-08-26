/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.interceptor;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.contrib.interceptor.common.ComposableInterceptor;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.data.DelegatingSpanData;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InterceptableSpanExporterTest {
  private InMemorySpanExporter memorySpanExporter;
  private Tracer tracer;
  private ComposableInterceptor<SpanData> interceptor;

  @BeforeEach
  void setUp() {
    memorySpanExporter = InMemorySpanExporter.create();
    interceptor = new ComposableInterceptor<>();
    tracer =
        SdkTracerProvider.builder()
            .addSpanProcessor(
                SimpleSpanProcessor.create(
                    new InterceptableSpanExporter(memorySpanExporter, interceptor)))
            .build()
            .get("TestScope");
  }

  @Test
  void verifySpanModification() {
    interceptor.add(
        item -> {
          ModifiableSpanData modified = new ModifiableSpanData(item);
          modified.attributes.put("global.attr", "from interceptor");
          return modified;
        });

    tracer.spanBuilder("Test span").setAttribute("local.attr", 10).startSpan().end();

    List<SpanData> finishedSpanItems = memorySpanExporter.getFinishedSpanItems();
    assertThat(finishedSpanItems.size()).isEqualTo(1);
    SpanData spanData = finishedSpanItems.get(0);
    assertThat(spanData.getAttributes().size()).isEqualTo(2);
    assertThat(spanData.getAttributes().get(AttributeKey.stringKey("global.attr")))
        .isEqualTo("from interceptor");
    assertThat(spanData.getAttributes().get(AttributeKey.longKey("local.attr"))).isEqualTo(10L);
  }

  @Test
  void verifySpanFiltering() {
    interceptor.add(
        item -> {
          if (item.getName().contains("deleted")) {
            return null;
          }
          return item;
        });

    tracer.spanBuilder("One span").startSpan().end();
    tracer.spanBuilder("This will get deleted").startSpan().end();
    tracer.spanBuilder("Another span").startSpan().end();

    List<SpanData> finishedSpanItems = memorySpanExporter.getFinishedSpanItems();
    assertThat(finishedSpanItems.size()).isEqualTo(2);
    assertThat(finishedSpanItems.get(0).getName()).isEqualTo("One span");
    assertThat(finishedSpanItems.get(1).getName()).isEqualTo("Another span");
  }

  private static class ModifiableSpanData extends DelegatingSpanData {
    private final AttributesBuilder attributes = Attributes.builder();

    ModifiableSpanData(SpanData delegate) {
      super(delegate);
    }

    @Override
    public Attributes getAttributes() {
      return attributes.putAll(super.getAttributes()).build();
    }
  }
}
