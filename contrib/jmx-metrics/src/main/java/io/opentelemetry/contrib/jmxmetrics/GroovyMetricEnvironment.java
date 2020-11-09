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

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Labels;
import io.opentelemetry.api.metrics.DoubleCounter;
import io.opentelemetry.api.metrics.DoubleSumObserver;
import io.opentelemetry.api.metrics.DoubleUpDownCounter;
import io.opentelemetry.api.metrics.DoubleUpDownSumObserver;
import io.opentelemetry.api.metrics.DoubleValueObserver;
import io.opentelemetry.api.metrics.DoubleValueRecorder;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.LongSumObserver;
import io.opentelemetry.api.metrics.LongUpDownCounter;
import io.opentelemetry.api.metrics.LongUpDownSumObserver;
import io.opentelemetry.api.metrics.LongValueObserver;
import io.opentelemetry.api.metrics.LongValueRecorder;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.exporter.logging.LoggingMetricExporter;
import io.opentelemetry.exporter.otlp.OtlpGrpcMetricExporter;
import io.opentelemetry.exporter.prometheus.PrometheusCollector;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.opentelemetry.sdk.metrics.export.MetricProducer;
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricExporter;
import io.prometheus.client.exporter.HTTPServer;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;

public class GroovyMetricEnvironment {

  private final Meter meter;
  private MetricExporter exporter;
  private HTTPServer prometheusServer;

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
    meter = OpenTelemetry.getGlobalMeter(instrumentationName, instrumentationVersion);

    switch (config.exporterType.toLowerCase()) {
      case "otlp":
        exporter = OtlpGrpcMetricExporter.builder().readProperties(config.properties).build();
        break;
      case "prometheus":
        configurePrometheus(config);
        break;
      case "inmemory":
        exporter = InMemoryMetricExporter.create();
        break;
      default:
        exporter = new LoggingMetricExporter();
        break;
    }
  }

  /**
   * Configures with default meter identifiers.
   *
   * @param config - used to establish exporter type (logging by default) and connection info
   */
  public GroovyMetricEnvironment(final JmxConfig config) {
    this(config, "io.opentelemetry.contrib.jmxmetrics", "0.0.1");
  }

  private static MetricProducer getMetricProducer() {
    return OpenTelemetrySdk.getGlobalMeterProvider().getMetricProducer();
  }

  private void configurePrometheus(final JmxConfig config) {
    PrometheusCollector.builder().setMetricProducer(getMetricProducer()).buildAndRegister();
    try {
      prometheusServer =
          new HTTPServer(config.prometheusExporterHost, config.prometheusExporterPort);
    } catch (IOException e) {
      throw new ConfigurationException("Cannot configure prometheus exporter server:", e);
    }
  }

  /** Will collect all metrics from OpenTelemetrySdk and export via configured exporter. */
  public void exportMetrics() {
    if (exporter != null) {
      Collection<MetricData> md = getMetricProducer().collectAllMetrics();
      exporter.export(md);
    }
  }

  public void shutdown() {
    if (prometheusServer != null) {
      prometheusServer.stop();
    }
  }

  protected static Labels mapToLabels(final Map<String, String> labelMap) {
    Labels.Builder labels = new Labels.Builder();
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
   * @return new or memoized {@link DoubleSumObserver}
   */
  public DoubleSumObserver getDoubleSumObserver(
      final String name, final String description, final String unit) {
    return meter.doubleSumObserverBuilder(name).setDescription(description).setUnit(unit).build();
  }

  /**
   * Build or retrieve previously registered {@link LongSumObserver}.
   *
   * @param name - metric name
   * @param description metric description
   * @param unit - metric unit
   * @return new or memoized {@link LongSumObserver}
   */
  public LongSumObserver getLongSumObserver(
      final String name, final String description, final String unit) {
    return meter.longSumObserverBuilder(name).setDescription(description).setUnit(unit).build();
  }

  /**
   * Build or retrieve previously registered {@link DoubleUpDownSumObserver}.
   *
   * @param name - metric name
   * @param description metric description
   * @param unit - metric unit
   * @return new or memoized {@link DoubleUpDownSumObserver}
   */
  public DoubleUpDownSumObserver getDoubleUpDownSumObserver(
      final String name, final String description, final String unit) {
    return meter
        .doubleUpDownSumObserverBuilder(name)
        .setDescription(description)
        .setUnit(unit)
        .build();
  }

  /**
   * Build or retrieve previously registered {@link LongUpDownSumObserver}.
   *
   * @param name - metric name
   * @param description metric description
   * @param unit - metric unit
   * @return new or memoized {@link LongUpDownSumObserver}
   */
  public LongUpDownSumObserver getLongUpDownSumObserver(
      final String name, final String description, final String unit) {
    return meter
        .longUpDownSumObserverBuilder(name)
        .setDescription(description)
        .setUnit(unit)
        .build();
  }

  /**
   * Build or retrieve previously registered {@link DoubleValueObserver}.
   *
   * @param name - metric name
   * @param description metric description
   * @param unit - metric unit
   * @return new or memoized {@link DoubleValueObserver}
   */
  public DoubleValueObserver getDoubleValueObserver(
      final String name, final String description, final String unit) {
    return meter.doubleValueObserverBuilder(name).setDescription(description).setUnit(unit).build();
  }

  /**
   * Build or retrieve previously registered {@link LongValueObserver}.
   *
   * @param name - metric name
   * @param description metric description
   * @param unit - metric unit
   * @return new or memoized {@link LongValueObserver}
   */
  public LongValueObserver getLongValueObserver(
      final String name, final String description, final String unit) {
    return meter.longValueObserverBuilder(name).setDescription(description).setUnit(unit).build();
  }
}
