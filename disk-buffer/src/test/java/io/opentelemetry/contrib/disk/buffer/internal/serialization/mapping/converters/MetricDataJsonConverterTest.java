package io.opentelemetry.contrib.disk.buffer.internal.serialization.mapping.converters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.opentelemetry.contrib.disk.buffer.internal.serialization.models.metrics.MetricDataJson;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.models.metrics.data.ExponentialHistogram;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.models.metrics.data.Gauge;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.models.metrics.data.Histogram;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.models.metrics.data.Sum;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.models.metrics.data.Summary;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.models.metrics.impl.ExponentialHistogramMetric;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.models.metrics.impl.GaugeMetric;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.models.metrics.impl.HistogramMetric;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.models.metrics.impl.SumMetric;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.models.metrics.impl.SummaryMetric;
import io.opentelemetry.contrib.disk.buffer.testutils.BaseJsonSerializationTest;
import org.junit.jupiter.api.Test;

class MetricDataJsonConverterTest extends BaseJsonSerializationTest<MetricDataJson> {
  private static final String METRIC_NAME = "metricName";
  private static final String METRIC_DESCRIPTION = "metricDescription";
  private static final String METRIC_UNIT = "metricUnit";

  @Test
  public void verifySumMetricSerialization() {
    SumMetric metric = new SumMetric();
    setBaseValues(metric);
    metric.sum = new Sum();

    byte[] serialized = serialize(metric);

    assertTrue(deserialize(serialized) instanceof SumMetric);
    assertBaseValues(metric);
  }

  @Test
  public void verifyGaugeMetricSerialization() {
    GaugeMetric metric = new GaugeMetric();
    setBaseValues(metric);
    metric.gauge = new Gauge();

    byte[] serialized = serialize(metric);

    assertTrue(deserialize(serialized) instanceof GaugeMetric);
    assertBaseValues(metric);
  }

  @Test
  public void verifyHistogramMetricSerialization() {
    HistogramMetric metric = new HistogramMetric();
    setBaseValues(metric);
    metric.histogram = new Histogram();

    byte[] serialized = serialize(metric);

    assertTrue(deserialize(serialized) instanceof HistogramMetric);
    assertBaseValues(metric);
  }

  @Test
  public void verifySummaryMetricSerialization() {
    SummaryMetric metric = new SummaryMetric();
    setBaseValues(metric);
    metric.summary = new Summary();

    byte[] serialized = serialize(metric);

    assertTrue(deserialize(serialized) instanceof SummaryMetric);
    assertBaseValues(metric);
  }

  @Test
  public void verifyExponentialHistogramMetricSerialization() {
    ExponentialHistogramMetric metric = new ExponentialHistogramMetric();
    setBaseValues(metric);
    metric.exponentialHistogram = new ExponentialHistogram();

    byte[] serialized = serialize(metric);

    assertTrue(deserialize(serialized) instanceof ExponentialHistogramMetric);
    assertBaseValues(metric);
  }

  private static void assertBaseValues(MetricDataJson metric) {
    assertEquals(METRIC_NAME, metric.name);
    assertEquals(METRIC_DESCRIPTION, metric.description);
    assertEquals(METRIC_UNIT, metric.unit);
  }

  private static void setBaseValues(MetricDataJson metric) {
    metric.name = METRIC_NAME;
    metric.description = METRIC_DESCRIPTION;
    metric.unit = METRIC_UNIT;
  }

  @Override
  protected Class<MetricDataJson> getTargetClass() {
    return MetricDataJson.class;
  }
}
