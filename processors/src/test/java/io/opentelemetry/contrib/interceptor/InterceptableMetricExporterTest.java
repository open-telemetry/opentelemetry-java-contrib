package io.opentelemetry.contrib.interceptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.sdk.common.InstrumentationScopeInfo;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.data.Data;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.data.MetricDataType;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricExporter;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InterceptableMetricExporterTest {
  private InMemoryMetricExporter memoryMetricExporter;
  private SdkMeterProvider meterProvider;
  private Meter meter;
  private InterceptableMetricExporter interceptable;

  @BeforeEach
  public void setUp() {
    memoryMetricExporter = InMemoryMetricExporter.create();
    interceptable = new InterceptableMetricExporter(memoryMetricExporter);
    meterProvider =
        SdkMeterProvider.builder()
            .registerMetricReader(PeriodicMetricReader.create(interceptable))
            .build();
    meter = meterProvider.get("TestScope");
  }

  @Test
  public void verifyMetricModification() {
    interceptable.addInterceptor(
        item -> {
          ModifiableMetricData modified = new ModifiableMetricData(item);
          modified.name = "ModifiedName";
          return modified;
        });

    meter.counterBuilder("OneCounter").build().add(1);
    meterProvider.forceFlush();

    List<MetricData> finishedMetricItems = memoryMetricExporter.getFinishedMetricItems();
    assertEquals(1, finishedMetricItems.size());
    assertEquals("ModifiedName", finishedMetricItems.get(0).getName());
  }

  @Test
  public void verifyMetricFiltering() {
    interceptable.addInterceptor(
        item -> {
          if (item.getName().contains("Deleted")) {
            return null;
          }
          return item;
        });

    meter.counterBuilder("OneCounter").build().add(1);
    meter.counterBuilder("DeletedCounter").build().add(1);
    meter.counterBuilder("AnotherCounter").build().add(1);
    meterProvider.forceFlush();

    List<MetricData> finishedMetricItems = memoryMetricExporter.getFinishedMetricItems();
    assertEquals(2, finishedMetricItems.size());
    List<String> names = new ArrayList<>();
    for (MetricData item : finishedMetricItems) {
      names.add(item.getName());
    }
    assertThat(names).containsExactlyInAnyOrder("OneCounter", "AnotherCounter");
  }

  private static class ModifiableMetricData implements MetricData {
    private final MetricData delegate;
    private String name;

    private ModifiableMetricData(MetricData delegate) {
      this.delegate = delegate;
    }

    @Override
    public Resource getResource() {
      return delegate.getResource();
    }

    @Override
    public InstrumentationScopeInfo getInstrumentationScopeInfo() {
      return delegate.getInstrumentationScopeInfo();
    }

    @Override
    public String getName() {
      if (name != null) {
        return name;
      }
      return delegate.getName();
    }

    @Override
    public String getDescription() {
      return delegate.getDescription();
    }

    @Override
    public String getUnit() {
      return delegate.getUnit();
    }

    @Override
    public MetricDataType getType() {
      return delegate.getType();
    }

    @Override
    public Data<?> getData() {
      return delegate.getData();
    }
  }
}
