/*
 * Copyright The OpenTelemetry Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.opentelemetry.contrib.jmxmetrics;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.metrics.AsynchronousInstrument;
import io.opentelemetry.api.metrics.DoubleCounter;
import io.opentelemetry.api.metrics.DoubleSumObserver;
import io.opentelemetry.api.metrics.DoubleUpDownCounter;
import io.opentelemetry.api.metrics.DoubleUpDownSumObserver;
import io.opentelemetry.api.metrics.DoubleValueObserver;
import io.opentelemetry.api.metrics.DoubleValueRecorder;
import io.opentelemetry.api.metrics.GlobalMetricsProvider;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.LongSumObserver;
import io.opentelemetry.api.metrics.LongUpDownCounter;
import io.opentelemetry.api.metrics.LongUpDownSumObserver;
import io.opentelemetry.api.metrics.LongValueObserver;
import io.opentelemetry.api.metrics.LongValueRecorder;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.common.Labels;
import io.opentelemetry.api.metrics.common.LabelsBuilder;
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
  private final Map<Integer, AtomicReference<Consumer<AsynchronousInstrument.LongResult>>>
      longUpdaterRegistry = new ConcurrentHashMap<>();
  private final Map<Integer, AtomicReference<Consumer<AsynchronousInstrument.DoubleResult>>>
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
        meterProvider = (SdkMeterProvider) GlobalMetricsProvider.get();
        break;
      default: // inmemory fallback
        meterProvider = SdkMeterProvider.builder().buildAndRegisterGlobal();
        exporter = InMemoryMetricExporter.create();
    }

    meter = meterProvider.get(instrumentationName, instrumentationVersion);
  }

  /**
   * Configures with default meter identifiers.
   *
   * @param config - used to establish exporter type (logging by default) and connection info
   */
  public GroovyMetricEnvironment(final JmxConfig config) {
    this(config, "io.opentelemetry.contrib.jmxmetrics", "1.0.0-alpha");
  }

  /** Will collect all metrics from OpenTelemetrySdk and export via configured exporter. */
  public void exportMetrics() {
    if (exporter != null) {
      Collection<MetricData> md = meterProvider.collectAllMetrics();
      exporter.export(md);
    }
  }

  protected static Labels mapToLabels(final Map<String, String> labelMap) {
    LabelsBuilder labels = Labels.builder();
    if (labelMap != null) {
      for (Map.Entry<String, String> kv : labelMap.entrySet()) {
        labels.put(kv.getKey(), kv.getValue());
      }
    }
    return labels.build();
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
    return meter.doubleCounterBuilder(name).setDescription(description).setUnit(unit).build();
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
    return meter.longCounterBuilder(name).setDescription(description).setUnit(unit).build();
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
    return meter.doubleUpDownCounterBuilder(name).setDescription(description).setUnit(unit).build();
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
    return meter.longUpDownCounterBuilder(name).setDescription(description).setUnit(unit).build();
  }

  /**
   * Build or retrieve previously registered {@link DoubleValueRecorder}.
   *
   * @param name - metric name
   * @param description metric description
   * @param unit - metric unit
   * @return new or memoized {@link DoubleValueRecorder}
   */
  public DoubleValueRecorder getDoubleValueRecorder(
      final String name, final String description, final String unit) {
    return meter.doubleValueRecorderBuilder(name).setDescription(description).setUnit(unit).build();
  }

  /**
   * Build or retrieve previously registered {@link LongValueRecorder}.
   *
   * @param name - metric name
   * @param description metric description
   * @param unit - metric unit
   * @return new or memoized {@link LongValueRecorder}
   */
  public LongValueRecorder getLongValueRecorder(
      final String name, final String description, final String unit) {
    return meter.longValueRecorderBuilder(name).setDescription(description).setUnit(unit).build();
  }

  /**
   * Build or retrieve previously registered {@link DoubleSumObserver}.
   *
   * @param name - metric name
   * @param description metric description
   * @param unit - metric unit
   * @param updater - the value updater
   * @return new or memoized {@link DoubleSumObserver}
   */
  public DoubleSumObserver getDoubleSumObserver(
      final String name,
      final String description,
      final String unit,
      final Consumer<AsynchronousInstrument.DoubleResult> updater) {
    return meter
        .doubleSumObserverBuilder(name)
        .setDescription(description)
        .setUnit(unit)
        .setUpdater(
            proxiedDoubleObserver(name, description, unit, InstrumentType.SUM_OBSERVER, updater))
        .build();
  }

  /**
   * Build or retrieve previously registered {@link LongSumObserver}.
   *
   * @param name - metric name
   * @param description metric description
   * @param unit - metric unit
   * @param updater - the value updater
   * @return new or memoized {@link LongSumObserver}
   */
  public LongSumObserver getLongSumObserver(
      final String name,
      final String description,
      final String unit,
      final Consumer<AsynchronousInstrument.LongResult> updater) {
    return meter
        .longSumObserverBuilder(name)
        .setDescription(description)
        .setUnit(unit)
        .setUpdater(
            proxiedLongObserver(name, description, unit, InstrumentType.SUM_OBSERVER, updater))
        .build();
  }

  /**
   * Build or retrieve previously registered {@link DoubleUpDownSumObserver}.
   *
   * @param name - metric name
   * @param description metric description
   * @param unit - metric unit
   * @param updater - the value updater
   * @return new or memoized {@link DoubleUpDownSumObserver}
   */
  public DoubleUpDownSumObserver getDoubleUpDownSumObserver(
      final String name,
      final String description,
      final String unit,
      final Consumer<AsynchronousInstrument.DoubleResult> updater) {
    return meter
        .doubleUpDownSumObserverBuilder(name)
        .setDescription(description)
        .setUnit(unit)
        .setUpdater(
            proxiedDoubleObserver(
                name, description, unit, InstrumentType.UP_DOWN_SUM_OBSERVER, updater))
        .build();
  }

  /**
   * Build or retrieve previously registered {@link LongUpDownSumObserver}.
   *
   * @param name - metric name
   * @param description metric description
   * @param unit - metric unit
   * @param updater - the value updater
   * @return new or memoized {@link LongUpDownSumObserver}
   */
  public LongUpDownSumObserver getLongUpDownSumObserver(
      final String name,
      final String description,
      final String unit,
      final Consumer<AsynchronousInstrument.LongResult> updater) {
    return meter
        .longUpDownSumObserverBuilder(name)
        .setDescription(description)
        .setUnit(unit)
        .setUpdater(
            proxiedLongObserver(
                name, description, unit, InstrumentType.UP_DOWN_SUM_OBSERVER, updater))
        .build();
  }

  /**
   * Build or retrieve previously registered {@link DoubleValueObserver}.
   *
   * @param name - metric name
   * @param description metric description
   * @param unit - metric unit
   * @param updater - the value updater
   * @return new or memoized {@link DoubleValueObserver}
   */
  public DoubleValueObserver getDoubleValueObserver(
      final String name,
      final String description,
      final String unit,
      final Consumer<AsynchronousInstrument.DoubleResult> updater) {
    return meter
        .doubleValueObserverBuilder(name)
        .setDescription(description)
        .setUnit(unit)
        .setUpdater(
            proxiedDoubleObserver(name, description, unit, InstrumentType.VALUE_OBSERVER, updater))
        .build();
  }

  /**
   * Build or retrieve previously registered {@link LongValueObserver}.
   *
   * @param name - metric name
   * @param description metric description
   * @param unit - metric unit
   * @param updater - the value updater
   * @return new or memoized {@link LongValueObserver}
   */
  public LongValueObserver getLongValueObserver(
      final String name,
      final String description,
      final String unit,
      final Consumer<AsynchronousInstrument.LongResult> updater) {
    return meter
        .longValueObserverBuilder(name)
        .setDescription(description)
        .setUnit(unit)
        .setUpdater(
            proxiedLongObserver(name, description, unit, InstrumentType.VALUE_OBSERVER, updater))
        .build();
  }

  private Consumer<AsynchronousInstrument.DoubleResult> proxiedDoubleObserver(
      final String name,
      final String description,
      final String unit,
      final InstrumentType instrumentType,
      final Consumer<AsynchronousInstrument.DoubleResult> updater) {
    InstrumentDescriptor descriptor =
        InstrumentDescriptor.create(
            name, description, unit, instrumentType, InstrumentValueType.DOUBLE);
    doubleUpdaterRegistry.putIfAbsent(descriptor.hashCode(), new AtomicReference<>());
    AtomicReference<Consumer<AsynchronousInstrument.DoubleResult>> existingUpdater =
        doubleUpdaterRegistry.get(descriptor.hashCode());
    existingUpdater.set(updater);
    return doubleResult -> {
      Consumer<AsynchronousInstrument.DoubleResult> existing = existingUpdater.get();
      existing.accept(doubleResult);
    };
  }

  private Consumer<AsynchronousInstrument.LongResult> proxiedLongObserver(
      final String name,
      final String description,
      final String unit,
      final InstrumentType instrumentType,
      final Consumer<AsynchronousInstrument.LongResult> updater) {
    InstrumentDescriptor descriptor =
        InstrumentDescriptor.create(
            name, description, unit, instrumentType, InstrumentValueType.LONG);
    longUpdaterRegistry.putIfAbsent(descriptor.hashCode(), new AtomicReference<>());
    AtomicReference<Consumer<AsynchronousInstrument.LongResult>> existingUpdater =
        longUpdaterRegistry.get(descriptor.hashCode());
    existingUpdater.set(updater);
    return longResult -> {
      Consumer<AsynchronousInstrument.LongResult> existing = existingUpdater.get();
      existing.accept(longResult);
    };
  }
}
