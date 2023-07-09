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
import io.opentelemetry.api.metrics.ObservableDoubleMeasurement;
import io.opentelemetry.api.metrics.ObservableLongMeasurement;
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
import java.util.logging.Logger;
import javax.annotation.Nullable;

public class GroovyMetricEnvironment {
  private static final Logger logger = Logger.getLogger(GroovyMetricEnvironment.class.getName());

  private final SdkMeterProvider meterProvider;
  private final Meter meter;

  // Observer updaters can only be specified in the builder as of v0.13.0, so to work with our model
  // of running groovy scripts on an interval a reference to the desired updater should be held and
  // updated w/ each instrument creation call.  Otherwise no observed changes in MBean availability
  // would be possible.  These registry stores are maps of instrument descriptor hashes to updater
  // consumer references.
  private final Map<Integer, AtomicReference<Consumer<ObservableLongMeasurement>>>
      longUpdaterRegistry = new ConcurrentHashMap<>();
  private final Map<Integer, AtomicReference<Consumer<ObservableDoubleMeasurement>>>
      doubleUpdaterRegistry = new ConcurrentHashMap<>();
  private final Map<Integer, Boolean> instrumentOnceRegistry = new ConcurrentHashMap<>();

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
    int descriptorHash =
        InstrumentDescriptor.create(
                name,
                description,
                unit,
                InstrumentType.OBSERVABLE_GAUGE,
                InstrumentValueType.DOUBLE)
            .hashCode();

    // Only build the instrument if it isn't already in the registry
    Boolean existingValue = instrumentOnceRegistry.putIfAbsent(descriptorHash, true);
    if (existingValue == null) {
      meter
          .gaugeBuilder(name)
          .setDescription(description)
          .setUnit(unit)
          .buildWithCallback(proxiedDoubleObserver(descriptorHash, updater));
    } else {
      // If the instrument has already been built with the appropriate proxied observer,
      // update the registry so that the callback has the appropriate updater function
      AtomicReference<Consumer<ObservableDoubleMeasurement>> existingUpdater =
          doubleUpdaterRegistry.get(descriptorHash);
      existingUpdater.set(updater);
    }
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
    int descriptorHash =
        InstrumentDescriptor.create(
                name, description, unit, InstrumentType.OBSERVABLE_GAUGE, InstrumentValueType.LONG)
            .hashCode();

    // Only build the instrument if it isn't already in the registry
    Boolean existingValue = instrumentOnceRegistry.putIfAbsent(descriptorHash, true);
    if (existingValue == null) {
      meter
          .gaugeBuilder(name)
          .ofLongs()
          .setDescription(description)
          .setUnit(unit)
          .buildWithCallback(proxiedLongObserver(descriptorHash, updater));
    } else {
      // If the instrument has already been built with the appropriate proxied observer,
      // update the registry so that the callback has the appropriate updater function
      AtomicReference<Consumer<ObservableLongMeasurement>> existingUpdater =
          longUpdaterRegistry.get(descriptorHash);
      existingUpdater.set(updater);
    }
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
    int descriptorHash =
        InstrumentDescriptor.create(
                name,
                description,
                unit,
                InstrumentType.OBSERVABLE_COUNTER,
                InstrumentValueType.DOUBLE)
            .hashCode();

    // Only build the instrument if it isn't already in the registry
    Boolean existingValue = instrumentOnceRegistry.putIfAbsent(descriptorHash, true);
    if (existingValue == null) {
      meter
          .counterBuilder(name)
          .ofDoubles()
          .setDescription(description)
          .setUnit(unit)
          .buildWithCallback(proxiedDoubleObserver(descriptorHash, updater));
    } else {
      // If the instrument has already been built with the appropriate proxied observer,
      // update the registry so that the callback has the appropriate updater function
      AtomicReference<Consumer<ObservableDoubleMeasurement>> existingUpdater =
          doubleUpdaterRegistry.get(descriptorHash);
      existingUpdater.set(updater);
    }
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
    int descriptorHash =
        InstrumentDescriptor.create(
                name,
                description,
                unit,
                InstrumentType.OBSERVABLE_COUNTER,
                InstrumentValueType.LONG)
            .hashCode();

    // Only build the instrument if it isn't already in the registry
    Boolean existingValue = instrumentOnceRegistry.putIfAbsent(descriptorHash, true);
    if (existingValue == null) {
      meter
          .counterBuilder(name)
          .setDescription(description)
          .setUnit(unit)
          .buildWithCallback(proxiedLongObserver(descriptorHash, updater));
    } else {
      // If the instrument has already been built with the appropriate proxied observer,
      // update the registry so that the callback has the appropriate updater function
      AtomicReference<Consumer<ObservableLongMeasurement>> existingUpdater =
          longUpdaterRegistry.get(descriptorHash);
      existingUpdater.set(updater);
    }
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
    int descriptorHash =
        InstrumentDescriptor.create(
                name,
                description,
                unit,
                InstrumentType.OBSERVABLE_UP_DOWN_COUNTER,
                InstrumentValueType.DOUBLE)
            .hashCode();

    // Only build the instrument if it isn't already in the registry
    Boolean existingValue = instrumentOnceRegistry.putIfAbsent(descriptorHash, true);
    if (existingValue == null) {
      meter
          .upDownCounterBuilder(name)
          .ofDoubles()
          .setDescription(description)
          .setUnit(unit)
          .buildWithCallback(proxiedDoubleObserver(descriptorHash, updater));
    } else {
      // If the instrument has already been built with the appropriate proxied observer,
      // update the registry so that the callback has the appropriate updater function
      AtomicReference<Consumer<ObservableDoubleMeasurement>> existingUpdater =
          doubleUpdaterRegistry.get(descriptorHash);
      existingUpdater.set(updater);
    }
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
    int descriptorHash =
        InstrumentDescriptor.create(
                name,
                description,
                unit,
                InstrumentType.OBSERVABLE_UP_DOWN_COUNTER,
                InstrumentValueType.LONG)
            .hashCode();

    // Only build the instrument if it isn't already in the registry
    Boolean existingValue = instrumentOnceRegistry.putIfAbsent(descriptorHash, true);
    if (existingValue == null) {
      meter
          .upDownCounterBuilder(name)
          .setDescription(description)
          .setUnit(unit)
          .buildWithCallback(proxiedLongObserver(descriptorHash, updater));
    } else {
      // If the instrument has already been built with the appropriate proxied observer,
      // update the registry so that the callback has the appropriate updater function
      AtomicReference<Consumer<ObservableLongMeasurement>> existingUpdater =
          longUpdaterRegistry.get(descriptorHash);
      existingUpdater.set(updater);
    }
  }

  private Consumer<ObservableDoubleMeasurement> proxiedDoubleObserver(
      final int descriptorHash, final Consumer<ObservableDoubleMeasurement> updater) {
    doubleUpdaterRegistry.putIfAbsent(descriptorHash, new AtomicReference<>());
    AtomicReference<Consumer<ObservableDoubleMeasurement>> existingUpdater =
        doubleUpdaterRegistry.get(descriptorHash);
    existingUpdater.set(updater);
    return doubleResult -> {
      Consumer<ObservableDoubleMeasurement> existing =
          doubleUpdaterRegistry.get(descriptorHash).get();
      existing.accept(doubleResult);
    };
  }

  private Consumer<ObservableLongMeasurement> proxiedLongObserver(
      final int descriptorHash, final Consumer<ObservableLongMeasurement> updater) {
    longUpdaterRegistry.putIfAbsent(descriptorHash, new AtomicReference<>());
    AtomicReference<Consumer<ObservableLongMeasurement>> existingUpdater =
        longUpdaterRegistry.get(descriptorHash);
    existingUpdater.set(updater);
    return longResult -> {
      Consumer<ObservableLongMeasurement> existing = longUpdaterRegistry.get(descriptorHash).get();
      existing.accept(longResult);
    };
  }
}
