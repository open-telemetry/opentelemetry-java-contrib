/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxmetrics;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.metrics.DoubleCounter;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.DoubleUpDownCounter;
import io.opentelemetry.api.metrics.GlobalMeterProvider;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.LongHistogram;
import io.opentelemetry.api.metrics.LongUpDownCounter;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.ObservableDoubleMeasurement;
import io.opentelemetry.api.metrics.ObservableLongMeasurement;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.common.InstrumentDescriptor;
import io.opentelemetry.sdk.metrics.common.InstrumentType;
import io.opentelemetry.sdk.metrics.common.InstrumentValueType;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.opentelemetry.sdk.metrics.testing.InMemoryMetricExporter;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import javax.annotation.Nullable;

public class GroovyMetricEnvironment {

  private final SdkMeterProvider meterProvider;
  private final Meter meter;

  // will only be `inmemory` since otel-java autoconfigure sdk extension manages other exporters
  private MetricExporter exporter;

  // Observer updaters can only be specified in the builder as of v0.13.0, so to work with our model
  // of running groovy scripts on an interval a reference to the desired updater should be held and
  // updated w/ each instrument creation call.  Otherwise no observed changes in MBean availability
  // would be possible.  These registry stores are maps of instrument descriptor hashes to updater
  // consumer references.
  private final Map<Integer, AtomicReference<Consumer<ObservableLongMeasurement>>>
      longUpdaterRegistry = new ConcurrentHashMap<>();
  private final Map<Integer, AtomicReference<Consumer<ObservableDoubleMeasurement>>>
      doubleUpdaterRegistry = new ConcurrentHashMap<>();

  /**
   * A central context for creating and exporting metrics, to be used by groovy scripts via {@link
   * io.opentelemetry.contrib.jmxmetrics.OtelHelper}.
   *
   * @param config - used to establish exporter type (logging by default) and connection info
   * @param instrumentationName - meter's instrumentationName
   * @param instrumentationVersion - meter's instrumentationVersion
   */
  public GroovyMetricEnvironment(
      final JmxConfig config,
      final String instrumentationName,
      final String instrumentationVersion) {

    switch (config.metricsExporterType.toLowerCase()) {
      case "otlp":
      case "prometheus":
      case "logging":
        // no need for autoconfigure sdk-extension to enable default traces exporter
        config.properties.setProperty("otel.traces.exporter", "none");
        // merge our sdk-supported properties to utilize autoconfigure features
        config.properties.forEach(
            (k, value) -> {
              String key = k.toString();
              if (key.startsWith("otel.") && !key.startsWith("otel.jmx")) {
                System.setProperty(key, value.toString());
              }
            });
        // this call will dynamically load the autoconfigure extension
        // and take care of provider and exporter creation for us based on system properties.
        GlobalOpenTelemetry.get();
        meterProvider = (SdkMeterProvider) GlobalMeterProvider.get();
        break;
      default: // inmemory fallback
        meterProvider = SdkMeterProvider.builder().buildAndRegisterGlobal();
        exporter = InMemoryMetricExporter.create();
    }

    meter = meterProvider.get(instrumentationName, instrumentationVersion, null);
  }

  // Visible for testing
  GroovyMetricEnvironment(SdkMeterProvider meterProvider, String instrumentationName) {
    this.meterProvider = meterProvider;
    meter = meterProvider.meterBuilder(instrumentationName).build();
  }

  /**
   * Configures with default meter identifiers.
   *
   * @param config - used to establish exporter type (logging by default) and connection info
   */
  public GroovyMetricEnvironment(final JmxConfig config) {
    this(
        config,
        "io.opentelemetry.contrib.jmxmetrics",
        GroovyMetricEnvironment.class.getPackage().getImplementationVersion());
  }

  /** Will collect all metrics from OpenTelemetrySdk and export via configured exporter. */
  public void exportMetrics() {
    if (exporter != null) {
      Collection<MetricData> md = meterProvider.collectAllMetrics();
      exporter.export(md);
    }
  }

  protected static Attributes mapToAttributes(@Nullable final Map<String, String> labelMap) {
    if (labelMap == null) {
      return Attributes.empty();
    }
    AttributesBuilder attrs = Attributes.builder();
    for (Map.Entry<String, String> kv : labelMap.entrySet()) {
      attrs.put(kv.getKey(), kv.getValue());
    }
    return attrs.build();
  }

  /**
   * Build or retrieve previously registered {@link DoubleCounter}.
   *
   * @param name - metric name
   * @param description metric description
   * @param unit - metric unit
   * @return new or memoized {@link DoubleCounter}
   */
  public DoubleCounter getDoubleCounter(
      final String name, final String description, final String unit) {
    return meter.counterBuilder(name).setDescription(description).setUnit(unit).ofDoubles().build();
  }

  /**
   * Build or retrieve previously registered {@link LongCounter}.
   *
   * @param name - metric name
   * @param description metric description
   * @param unit - metric unit
   * @return new or memoized {@link LongCounter}
   */
  public LongCounter getLongCounter(
      final String name, final String description, final String unit) {
    return meter.counterBuilder(name).setDescription(description).setUnit(unit).build();
  }

  /**
   * Build or retrieve previously registered {@link DoubleUpDownCounter}.
   *
   * @param name - metric name
   * @param description metric description
   * @param unit - metric unit
   * @return new or memoized {@link DoubleUpDownCounter}
   */
  public DoubleUpDownCounter getDoubleUpDownCounter(
      final String name, final String description, final String unit) {
    return meter
        .upDownCounterBuilder(name)
        .setDescription(description)
        .setUnit(unit)
        .ofDoubles()
        .build();
  }

  /**
   * Build or retrieve previously registered {@link LongUpDownCounter}.
   *
   * @param name - metric name
   * @param description metric description
   * @param unit - metric unit
   * @return new or memoized {@link LongUpDownCounter}
   */
  public LongUpDownCounter getLongUpDownCounter(
      final String name, final String description, final String unit) {
    return meter.upDownCounterBuilder(name).setDescription(description).setUnit(unit).build();
  }

  /**
   * Build or retrieve previously registered {@link DoubleHistogram}.
   *
   * @param name - metric name
   * @param description metric description
   * @param unit - metric unit
   * @return new or memoized {@link DoubleHistogram}
   */
  public DoubleHistogram getDoubleHistogram(
      final String name, final String description, final String unit) {
    return meter.histogramBuilder(name).setDescription(description).setUnit(unit).build();
  }

  /**
   * Build or retrieve previously registered {@link }.
   *
   * @param name - metric name
   * @param description metric description
   * @param unit - metric unit
   * @return new or memoized {@link LongHistogram}
   */
  public LongHistogram getLongHistogram(
      final String name, final String description, final String unit) {
    return meter.histogramBuilder(name).setDescription(description).setUnit(unit).ofLongs().build();
  }

  /**
   * Register a double observable gauge.
   *
   * @param name - metric name
   * @param description metric description
   * @param unit - metric unit
   * @param updater - the value updater
   */
  public void registerDoubleValueCallback(
      final String name,
      final String description,
      final String unit,
      final Consumer<ObservableDoubleMeasurement> updater) {
    meter
        .gaugeBuilder(name)
        .setDescription(description)
        .setUnit(unit)
        .buildWithCallback(
            proxiedDoubleObserver(
                name, description, unit, InstrumentType.OBSERVABLE_GAUGE, updater));
  }

  /**
   * Register a long observable gauge.
   *
   * @param name - metric name
   * @param description metric description
   * @param unit - metric unit
   * @param updater - the value updater
   */
  public void registerLongValueCallback(
      final String name,
      final String description,
      final String unit,
      final Consumer<ObservableLongMeasurement> updater) {
    meter
        .gaugeBuilder(name)
        .ofLongs()
        .setDescription(description)
        .setUnit(unit)
        .buildWithCallback(
            proxiedLongObserver(name, description, unit, InstrumentType.OBSERVABLE_GAUGE, updater));
  }

  /**
   * Register an observable double counter.
   *
   * @param name - metric name
   * @param description metric description
   * @param unit - metric unit
   * @param updater - the value updater
   */
  public void registerDoubleCounterCallback(
      final String name,
      final String description,
      final String unit,
      final Consumer<ObservableDoubleMeasurement> updater) {
    meter
        .counterBuilder(name)
        .ofDoubles()
        .setDescription(description)
        .setUnit(unit)
        .buildWithCallback(
            proxiedDoubleObserver(name, description, unit, InstrumentType.OBSERVABLE_SUM, updater));
  }

  /**
   * Register an observable long counter.
   *
   * @param name - metric name
   * @param description metric description
   * @param unit - metric unit
   * @param updater - the value updater
   */
  public void registerLongCounterCallback(
      final String name,
      final String description,
      final String unit,
      final Consumer<ObservableLongMeasurement> updater) {
    meter
        .counterBuilder(name)
        .setDescription(description)
        .setUnit(unit)
        .buildWithCallback(
            proxiedLongObserver(name, description, unit, InstrumentType.OBSERVABLE_SUM, updater));
  }

  /**
   * Register an observable double updown counter.
   *
   * @param name - metric name
   * @param description metric description
   * @param unit - metric unit
   * @param updater - the value updater
   */
  public void registerDoubleUpDownCounterCallback(
      final String name,
      final String description,
      final String unit,
      final Consumer<ObservableDoubleMeasurement> updater) {
    meter
        .upDownCounterBuilder(name)
        .ofDoubles()
        .setDescription(description)
        .setUnit(unit)
        .buildWithCallback(
            proxiedDoubleObserver(
                name, description, unit, InstrumentType.OBSERVABLE_UP_DOWN_SUM, updater));
  }

  /**
   * Register an observable long updown counter.
   *
   * @param name - metric name
   * @param description metric description
   * @param unit - metric unit
   * @param updater - the value updater
   */
  public void registerLongUpDownCounterCallback(
      final String name,
      final String description,
      final String unit,
      final Consumer<ObservableLongMeasurement> updater) {
    meter
        .upDownCounterBuilder(name)
        .setDescription(description)
        .setUnit(unit)
        .buildWithCallback(
            proxiedLongObserver(
                name, description, unit, InstrumentType.OBSERVABLE_UP_DOWN_SUM, updater));
  }

  private Consumer<ObservableDoubleMeasurement> proxiedDoubleObserver(
      final String name,
      final String description,
      final String unit,
      final InstrumentType instrumentType,
      final Consumer<ObservableDoubleMeasurement> updater) {
    InstrumentDescriptor descriptor =
        InstrumentDescriptor.create(
            name, description, unit, instrumentType, InstrumentValueType.DOUBLE);
    doubleUpdaterRegistry.putIfAbsent(descriptor.hashCode(), new AtomicReference<>());
    AtomicReference<Consumer<ObservableDoubleMeasurement>> existingUpdater =
        doubleUpdaterRegistry.get(descriptor.hashCode());
    existingUpdater.set(updater);
    return doubleResult -> {
      Consumer<ObservableDoubleMeasurement> existing = existingUpdater.get();
      existing.accept(doubleResult);
    };
  }

  private Consumer<ObservableLongMeasurement> proxiedLongObserver(
      final String name,
      final String description,
      final String unit,
      final InstrumentType instrumentType,
      final Consumer<ObservableLongMeasurement> updater) {
    InstrumentDescriptor descriptor =
        InstrumentDescriptor.create(
            name, description, unit, instrumentType, InstrumentValueType.LONG);
    longUpdaterRegistry.putIfAbsent(descriptor.hashCode(), new AtomicReference<>());
    AtomicReference<Consumer<ObservableLongMeasurement>> existingUpdater =
        longUpdaterRegistry.get(descriptor.hashCode());
    existingUpdater.set(updater);
    return longResult -> {
      Consumer<ObservableLongMeasurement> existing = existingUpdater.get();
      existing.accept(longResult);
    };
  }
}
