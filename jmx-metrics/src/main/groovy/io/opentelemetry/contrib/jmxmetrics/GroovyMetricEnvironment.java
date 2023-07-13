/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxmetrics;

import groovy.lang.Closure;
import groovy.lang.Tuple2;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.metrics.BatchCallback;
import io.opentelemetry.api.metrics.DoubleCounter;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.DoubleUpDownCounter;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.LongHistogram;
import io.opentelemetry.api.metrics.LongUpDownCounter;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.ObservableDoubleMeasurement;
import io.opentelemetry.api.metrics.ObservableLongMeasurement;
import io.opentelemetry.api.metrics.ObservableMeasurement;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.InstrumentType;
import io.opentelemetry.sdk.metrics.InstrumentValueType;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

public class GroovyMetricEnvironment {
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
  private final Map<Integer, AtomicReference<Closure<?>>> batchUpdaterRegistry =
      new ConcurrentHashMap<>();
  private final Map<Integer, Tuple2<BatchCallback, Set<ObservableMeasurement>>>
      batchCallbackRegistry = new ConcurrentHashMap<>();
  private final Map<Integer, ObservableMeasurement> instrumentOnceRegistry =
      new ConcurrentHashMap<>();

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
   * @return the ObservableDoubleMeasurement for the gauge
   */
  public ObservableDoubleMeasurement registerDoubleValueCallback(
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

    return registerCallback(
        doubleUpdaterRegistry,
        () -> meter.gaugeBuilder(name).setDescription(description).setUnit(unit).buildObserver(),
        descriptorHash,
        updater);
  }

  /**
   * Register a long observable gauge.
   *
   * @param name - metric name
   * @param description metric description
   * @param unit - metric unit
   * @param updater - the value updater
   * @return the ObservableLongMeasurement for the gauge
   */
  public ObservableLongMeasurement registerLongValueCallback(
      final String name,
      final String description,
      final String unit,
      final Consumer<ObservableLongMeasurement> updater) {
    int descriptorHash =
        InstrumentDescriptor.create(
                name, description, unit, InstrumentType.OBSERVABLE_GAUGE, InstrumentValueType.LONG)
            .hashCode();

    return registerCallback(
        longUpdaterRegistry,
        () ->
            meter
                .gaugeBuilder(name)
                .ofLongs()
                .setDescription(description)
                .setUnit(unit)
                .buildObserver(),
        descriptorHash,
        updater);
  }

  /**
   * Register an observable double counter.
   *
   * @param name - metric name
   * @param description metric description
   * @param unit - metric unit
   * @param updater - the value updater
   * @return the ObservableDoubleMeasurement for the counter
   */
  public ObservableDoubleMeasurement registerDoubleCounterCallback(
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

    return registerCallback(
        doubleUpdaterRegistry,
        () ->
            meter
                .counterBuilder(name)
                .setDescription(description)
                .setUnit(unit)
                .ofDoubles()
                .buildObserver(),
        descriptorHash,
        updater);
  }

  /**
   * Register an observable long counter.
   *
   * @param name - metric name
   * @param description metric description
   * @param unit - metric unit
   * @param updater - the value updater
   * @return the ObservableLongMeasurement for the counter
   */
  public ObservableLongMeasurement registerLongCounterCallback(
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

    return registerCallback(
        longUpdaterRegistry,
        () -> meter.counterBuilder(name).setDescription(description).setUnit(unit).buildObserver(),
        descriptorHash,
        updater);
  }

  /**
   * Register an observable double updown counter.
   *
   * @param name - metric name
   * @param description metric description
   * @param unit - metric unit
   * @param updater - the value updater
   * @return the ObservableDoubleMeasurement for the counter
   */
  public ObservableDoubleMeasurement registerDoubleUpDownCounterCallback(
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

    return registerCallback(
        doubleUpdaterRegistry,
        () ->
            meter
                .upDownCounterBuilder(name)
                .setDescription(description)
                .setUnit(unit)
                .ofDoubles()
                .buildObserver(),
        descriptorHash,
        updater);
  }

  /**
   * Register an observable long updown counter.
   *
   * @param name - metric name
   * @param description metric description
   * @param unit - metric unit
   * @param updater - the value updater
   * @return the ObservableLongMeasurement for the counter
   */
  public ObservableLongMeasurement registerLongUpDownCounterCallback(
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

    return registerCallback(
        longUpdaterRegistry,
        () ->
            meter
                .upDownCounterBuilder(name)
                .setDescription(description)
                .setUnit(unit)
                .buildObserver(),
        descriptorHash,
        updater);
  }

  private <T extends ObservableMeasurement> T registerCallback(
      final Map<Integer, AtomicReference<Consumer<T>>> registry,
      final Supplier<T> observerBuilder,
      final int descriptorHash,
      final Consumer<T> updater) {

    // Only build the instrument if it isn't already in the registry
    ObservableMeasurement obs = instrumentOnceRegistry.get(descriptorHash);
    if (obs == null) {
      T observer = observerBuilder.get();
      instrumentOnceRegistry.put(descriptorHash, observer);
      // If an updater was not provided, the measurement is expected to be added
      // to a group batchcallback using the registerBatchCallback function
      if (updater != null) {
        Consumer<T> cb = proxiedObserver(descriptorHash, registry, updater);
        meter.batchCallback(() -> cb.accept(observer), observer);
      }
      return observer;
    } else if (updater != null) {
      // If the instrument has already been built with the appropriate proxied observer,
      // update the registry so that the callback has the appropriate updater function
      registry.get(descriptorHash).set(updater);
    }

    return (T) obs;
  }

  /**
   * Register a collection of observables in a single batch callback
   *
   * @param identifier - object used to identify the callback to have only one callback
   * @param callback - closure that records measurements for the observables
   * @param measurement - first observable, the SDK expects this is always collected
   * @param additional - remaining observable, the SDK expects this is sometimes collected
   */
  public void registerBatchCallback(
      Object identifier,
      Closure<?> callback,
      ObservableMeasurement measurement,
      ObservableMeasurement... additional) {
    int hash = identifier.hashCode();
    // Store the callback in the registry so the proxied callback always runs the latest
    // metric collection closure
    batchUpdaterRegistry.putIfAbsent(hash, new AtomicReference<>());
    batchUpdaterRegistry.get(hash).set(callback);

    // collect the set of instruments into a set so we can compare to what's previously been
    // registered
    Set<ObservableMeasurement> instrumentSet =
        Arrays.stream(additional).collect(Collectors.toCollection(HashSet::new));
    instrumentSet.add(measurement);

    Tuple2<BatchCallback, Set<ObservableMeasurement>> existingCallback =
        batchCallbackRegistry.get(hash);
    // If this is our first attempt to register this callback or the list of relevant instruments
    // has changed, we need register the callback.
    if (existingCallback == null || !existingCallback.getV2().equals(instrumentSet)) {
      // If the callback has already been created, and we're here to update the set of instruments
      // make sure we close the previous callback
      if (existingCallback != null) {
        existingCallback.getV1().close();
      }
      batchCallbackRegistry.put(
          hash,
          new Tuple2<>(
              meter.batchCallback(
                  () -> batchUpdaterRegistry.get(hash).get().call(), measurement, additional),
              instrumentSet));
    }
  }

  private <T extends ObservableMeasurement> Consumer<T> proxiedObserver(
      final int descriptorHash,
      final Map<Integer, AtomicReference<Consumer<T>>> registry,
      final Consumer<T> updater) {
    registry.putIfAbsent(descriptorHash, new AtomicReference<>());
    registry.get(descriptorHash).set(updater);
    return result -> registry.get(descriptorHash).get().accept(result);
  }
}
