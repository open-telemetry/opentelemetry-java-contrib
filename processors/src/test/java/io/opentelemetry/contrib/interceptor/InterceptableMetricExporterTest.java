/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.interceptor;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.contrib.interceptor.common.ComposableInterceptor;
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
  private ComposableInterceptor<MetricData> interceptor;

  @BeforeEach
  void setUp() {
    memoryMetricExporter = InMemoryMetricExporter.create();
    interceptor = new ComposableInterceptor<>();
    meterProvider =
        SdkMeterProvider.builder()
            .registerMetricReader(
                PeriodicMetricReader.create(
                    new InterceptableMetricExporter(memoryMetricExporter, interceptor)))
            .build();
    meter = meterProvider.get("TestScope");
  }

  @Test
  void verifyMetricModification() {
    interceptor.add(
        item -> {
          ModifiableMetricData modified = new ModifiableMetricData(item);
          modified.name = "ModifiedName";
          return modified;
        });

    meter.counterBuilder("OneCounter").build().add(1);
    meterProvider.forceFlush();

    List<MetricData> finishedMetricItems = memoryMetricExporter.getFinishedMetricItems();
    assertThat(finishedMetricItems.size()).isEqualTo(1);
    assertThat(finishedMetricItems.get(0).getName()).isEqualTo("ModifiedName");
  }

  @Test
  void verifyMetricFiltering() {
    interceptor.add(
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
    assertThat(finishedMetricItems.size()).isEqualTo(2);
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
