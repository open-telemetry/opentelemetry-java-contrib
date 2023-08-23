package io.opentelemetry.contrib.interceptor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.trace.Tracer;
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
  private InterceptableSpanExporter interceptable;

  @BeforeEach
  public void setUp() {
    memorySpanExporter = InMemorySpanExporter.create();
    interceptable = new InterceptableSpanExporter(memorySpanExporter);
    tracer =
        SdkTracerProvider.builder()
            .addSpanProcessor(SimpleSpanProcessor.create(interceptable))
            .build()
            .get("TestScope");
  }

  @Test
  public void verifySpanModification() {
    interceptable.addInterceptor(
        item -> {
          ModifiableSpanData modified = new ModifiableSpanData(item);
          modified.attributes.put("global.attr", "from interceptor");
          return modified;
        });

    tracer.spanBuilder("Test span").setAttribute("local.attr", 10).startSpan().end();

    List<SpanData> finishedSpanItems = memorySpanExporter.getFinishedSpanItems();
    assertEquals(1, finishedSpanItems.size());
    SpanData spanData = finishedSpanItems.get(0);
    assertEquals(2, spanData.getAttributes().size());
    assertEquals(
        "from interceptor", spanData.getAttributes().get(AttributeKey.stringKey("global.attr")));
    assertEquals(10, spanData.getAttributes().get(AttributeKey.longKey("local.attr")));
  }

  @Test
  public void verifySpanFiltering() {
    interceptable.addInterceptor(
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
    assertEquals(2, finishedSpanItems.size());
    assertEquals("One span", finishedSpanItems.get(0).getName());
    assertEquals("Another span", finishedSpanItems.get(1).getName());
  }

  private static class ModifiableSpanData extends DelegatingSpanData {
    private final AttributesBuilder attributes = Attributes.builder();

    protected ModifiableSpanData(SpanData delegate) {
      super(delegate);
    }

    @Override
    public Attributes getAttributes() {
      return attributes.putAll(super.getAttributes()).build();
    }
  }
}
