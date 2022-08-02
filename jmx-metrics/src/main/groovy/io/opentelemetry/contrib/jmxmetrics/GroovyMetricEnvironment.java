/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxmetrics;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.metrics.DoubleCounter;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.DoubleUpDownCounter;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.LongHistogram;
import io.opentelemetry.api.metrics.LongUpDownCounter;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.ObservableDoubleCounter;
import io.opentelemetry.api.metrics.ObservableDoubleGauge;
import io.opentelemetry.api.metrics.ObservableDoubleMeasurement;
import io.opentelemetry.api.metrics.ObservableDoubleUpDownCounter;
import io.opentelemetry.api.metrics.ObservableLongCounter;
import io.opentelemetry.api.metrics.ObservableLongGauge;
import io.opentelemetry.api.metrics.ObservableLongMeasurement;
import io.opentelemetry.api.metrics.ObservableLongUpDownCounter;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.InstrumentType;
import io.opentelemetry.sdk.metrics.InstrumentValueType;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.annotation.Nullable;

public class GroovyMetricEnvironment {

  private final SdkMeterProvider meterProvider;
  private final Meter meter;

  private class Registry<T> {
    private final Map<Integer, AtomicReference<T>> registry = new ConcurrentHashMap<>();

    private T getOrSet(
        String name,
        String description,
        String unit,
        InstrumentType instrumentType,
        InstrumentValueType instrumentValueType,
        Function<Integer, T> createFunction) {
      InstrumentDescriptor descriptor =
          InstrumentDescriptor.create(name, description, unit, instrumentType, instrumentValueType);
      return registry
          .computeIfAbsent(
              descriptor.hashCode(),
              k -> {
                AtomicReference<T> at = new AtomicReference<T>();
                at.set(createFunction.apply(k));
                return at;
              })
          .get();
    }

    private AtomicReference<T> getExisting(
        String name,
        String description,
        String unit,
        InstrumentType instrumentType,
        InstrumentValueType instrumentValueType) {
      InstrumentDescriptor descriptor =
          InstrumentDescriptor.create(name, description, unit, instrumentType, instrumentValueType);
      registry.putIfAbsent(descriptor.hashCode(), new AtomicReference<>());
      return registry.get(descriptor.hashCode());
    }
  }

  // cache all instruments by their descriptors to prevent attempted duplicate creation
  private final Registry<DoubleCounter> doubleCounterRegistry = new Registry<>();
  private final Registry<LongCounter> longCounterRegistry = new Registry<>();
  private final Registry<DoubleUpDownCounter> doubleUpDownCounterRegistry = new Registry<>();
  private final Registry<LongUpDownCounter> longUpDownCounterRegistry = new Registry<>();
  private final Registry<DoubleHistogram> doubleHistogramRegistry = new Registry<>();
  private final Registry<LongHistogram> longHistogramRegistry = new Registry<>();
  private final Registry<ObservableDoubleGauge> observableDoubleGaugeRegistry = new Registry<>();
  private final Registry<ObservableLongGauge> observableLongGaugeRegistry = new Registry<>();
  private final Registry<ObservableDoubleCounter> observableDoubleCounterRegistry =
      new Registry<>();
  private final Registry<ObservableLongCounter> observableLongCounterRegistry = new Registry<>();
  private final Registry<ObservableDoubleUpDownCounter> observableDoubleUpDownCounterRegistry =
      new Registry<>();
  private final Registry<ObservableLongUpDownCounter> observableLongUpDownCounterRegistry =
      new Registry<>();

  // Observable consumers can only be specified in the builder as of v0.13.0, so to work with our
  // model
  // of running groovy scripts on an interval a reference to the desired updater should be held and
  // updated w/ each instrument creation call.  Otherwise no observed changes in MBean availability
  // would be possible.
  private final Registry<Consumer<ObservableLongMeasurement>> longUpdaterRegistry =
      new Registry<>();
  private final Registry<Consumer<ObservableDoubleMeasurement>> doubleUpdaterRegistry =
      new Registry<>();

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
        // call the autoconfigure extension and take care of provider and exporter creation for us
        // based on system properties.
        meterProvider =
            AutoConfiguredOpenTelemetrySdk.builder()
                .setResultAsGlobal(false)
                .addPropertiesSupplier(
                    () -> {
                      Map<String, String> properties = new HashMap<>();
                      // no need for autoconfigure sdk-extension to enable default traces exporter
                      properties.put("otel.traces.exporter", "none");
                      // expose config.properties to autoconfigure
                      config.properties.forEach(
                          (k, value) -> {
                            String key = k.toString();
                            if (key.startsWith("otel.") && !key.startsWith("otel.jmx")) {
                              properties.put(key, value.toString());
                            }
                          });
                      return properties;
                    })
                .build()
                .getOpenTelemetrySdk()
                .getSdkMeterProvider();
        break;
      default: // inmemory fallback
        meterProvider = SdkMeterProvider.builder().build();
    }

    meter =
        meterProvider
            .meterBuilder(instrumentationName)
            .setInstrumentationVersion(instrumentationVersion)
            .build();
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
  public void flush() {
    meterProvider.forceFlush().join(10, TimeUnit.SECONDS);
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
    return doubleCounterRegistry.getOrSet(
        name,
        description,
        unit,
        InstrumentType.COUNTER,
        InstrumentValueType.DOUBLE,
        k ->
            meter
                .counterBuilder(name)
                .setDescription(description)
                .setUnit(unit)
                .ofDoubles()
                .build());
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
    return longCounterRegistry.getOrSet(
        name,
        description,
        unit,
        InstrumentType.COUNTER,
        InstrumentValueType.LONG,
        k -> meter.counterBuilder(name).setDescription(description).setUnit(unit).build());
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
    return doubleUpDownCounterRegistry.getOrSet(
        name,
        description,
        unit,
        InstrumentType.UP_DOWN_COUNTER,
        InstrumentValueType.DOUBLE,
        k ->
            meter
                .upDownCounterBuilder(name)
                .setDescription(description)
                .setUnit(unit)
                .ofDoubles()
                .build());
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
    return longUpDownCounterRegistry.getOrSet(
        name,
        description,
        unit,
        InstrumentType.UP_DOWN_COUNTER,
        InstrumentValueType.LONG,
        k -> meter.upDownCounterBuilder(name).setDescription(description).setUnit(unit).build());
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
    return doubleHistogramRegistry.getOrSet(
        name,
        description,
        unit,
        InstrumentType.HISTOGRAM,
        InstrumentValueType.DOUBLE,
        k -> meter.histogramBuilder(name).setDescription(description).setUnit(unit).build());
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
    return longHistogramRegistry.getOrSet(
        name,
        description,
        unit,
        InstrumentType.HISTOGRAM,
        InstrumentValueType.LONG,
        k ->
            meter
                .histogramBuilder(name)
                .setDescription(description)
                .setUnit(unit)
                .ofLongs()
                .build());
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
    // we must invoke this every call to update the consumer's proxied value
    Consumer<ObservableDoubleMeasurement> pdo =
        proxiedDoubleObserver(name, description, unit, InstrumentType.OBSERVABLE_GAUGE, updater);
    observableDoubleGaugeRegistry.getOrSet(
        name,
        description,
        unit,
        InstrumentType.OBSERVABLE_GAUGE,
        InstrumentValueType.DOUBLE,
        k ->
            meter
                .gaugeBuilder(name)
                .setDescription(description)
                .setUnit(unit)
                .buildWithCallback(pdo));
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
    // we must invoke this every call to update the consumer's proxied value
    Consumer<ObservableLongMeasurement> pdo =
        proxiedLongObserver(name, description, unit, InstrumentType.OBSERVABLE_GAUGE, updater);
    observableLongGaugeRegistry.getOrSet(
        name,
        description,
        unit,
        InstrumentType.OBSERVABLE_GAUGE,
        InstrumentValueType.LONG,
        k ->
            meter
                .gaugeBuilder(name)
                .ofLongs()
                .setDescription(description)
                .setUnit(unit)
                .buildWithCallback(pdo));
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
    // we must invoke this every call to update the consumer's proxied value
    Consumer<ObservableDoubleMeasurement> pdo =
        proxiedDoubleObserver(name, description, unit, InstrumentType.OBSERVABLE_COUNTER, updater);
    observableDoubleCounterRegistry.getOrSet(
        name,
        description,
        unit,
        InstrumentType.OBSERVABLE_COUNTER,
        InstrumentValueType.DOUBLE,
        k ->
            meter
                .counterBuilder(name)
                .ofDoubles()
                .setDescription(description)
                .setUnit(unit)
                .buildWithCallback(pdo));
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
    // we must invoke this every call to update the consumer's proxied value
    Consumer<ObservableLongMeasurement> pdo =
        proxiedLongObserver(name, description, unit, InstrumentType.OBSERVABLE_COUNTER, updater);
    observableLongCounterRegistry.getOrSet(
        name,
        description,
        unit,
        InstrumentType.OBSERVABLE_COUNTER,
        InstrumentValueType.LONG,
        k ->
            meter
                .counterBuilder(name)
                .setDescription(description)
                .setUnit(unit)
                .buildWithCallback(pdo));
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
    // we must invoke this every call to update the consumer's proxied value
    Consumer<ObservableDoubleMeasurement> pdo =
        proxiedDoubleObserver(
            name, description, unit, InstrumentType.OBSERVABLE_UP_DOWN_COUNTER, updater);
    observableDoubleUpDownCounterRegistry.getOrSet(
        name,
        description,
        unit,
        InstrumentType.OBSERVABLE_UP_DOWN_COUNTER,
        InstrumentValueType.DOUBLE,
        k ->
            meter
                .upDownCounterBuilder(name)
                .ofDoubles()
                .setDescription(description)
                .setUnit(unit)
                .buildWithCallback(pdo));
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
    // we must invoke this every call to update the consumer's proxied value
    Consumer<ObservableLongMeasurement> pdo =
        proxiedLongObserver(
            name, description, unit, InstrumentType.OBSERVABLE_UP_DOWN_COUNTER, updater);
    observableLongUpDownCounterRegistry.getOrSet(
        name,
        description,
        unit,
        InstrumentType.OBSERVABLE_UP_DOWN_COUNTER,
        InstrumentValueType.LONG,
        k ->
            meter
                .upDownCounterBuilder(name)
                .setDescription(description)
                .setUnit(unit)
                .buildWithCallback(pdo));
  }

  private Consumer<ObservableDoubleMeasurement> proxiedDoubleObserver(
      final String name,
      final String description,
      final String unit,
      final InstrumentType instrumentType,
      final Consumer<ObservableDoubleMeasurement> updater) {
    AtomicReference<Consumer<ObservableDoubleMeasurement>> existingUpdater =
        doubleUpdaterRegistry.getExisting(
            name, description, unit, instrumentType, InstrumentValueType.DOUBLE);
    existingUpdater.set(updater);
    return doubleResult -> {
      existingUpdater.get().accept(doubleResult);
    };
  }

  private Consumer<ObservableLongMeasurement> proxiedLongObserver(
      final String name,
      final String description,
      final String unit,
      final InstrumentType instrumentType,
      final Consumer<ObservableLongMeasurement> updater) {
    AtomicReference<Consumer<ObservableLongMeasurement>> existingUpdater =
        longUpdaterRegistry.getExisting(
            name, description, unit, instrumentType, InstrumentValueType.LONG);
    existingUpdater.set(updater);
    return longResult -> {
      existingUpdater.get().accept(longResult);
    };
  }
}
