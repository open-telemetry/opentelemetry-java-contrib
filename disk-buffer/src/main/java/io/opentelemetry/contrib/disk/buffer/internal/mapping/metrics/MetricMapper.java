package io.opentelemetry.contrib.disk.buffer.internal.mapping.metrics;

import io.opentelemetry.contrib.disk.buffer.internal.mapping.metrics.models.MetricDataImpl;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.metrics.MetricDataJson;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.metrics.data.DataJson;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.metrics.data.ExponentialHistogram;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.metrics.data.Gauge;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.metrics.data.Histogram;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.metrics.data.Sum;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.metrics.data.Summary;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.metrics.datapoints.NumberDataPoint;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.metrics.impl.ExponentialHistogramMetric;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.metrics.impl.GaugeMetric;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.metrics.impl.HistogramMetric;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.metrics.impl.SumMetric;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.metrics.impl.SummaryMetric;
import io.opentelemetry.sdk.common.InstrumentationScopeInfo;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.data.MetricDataType;
import io.opentelemetry.sdk.resources.Resource;
import java.util.List;
import org.mapstruct.AfterMapping;
import org.mapstruct.BeanMapping;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

@Mapper(uses = MetricDataMapper.class)
public abstract class MetricMapper {

  public static final MetricMapper INSTANCE = Mappers.getMapper(MetricMapper.class);

  public MetricDataJson metricToJson(MetricData source) {
    MetricDataType type = source.getType();
    switch (type) {
      case LONG_GAUGE:
      case DOUBLE_GAUGE:
        return gaugeMetricToJson(source, type);
      case LONG_SUM:
      case DOUBLE_SUM:
        return sumMetricToJson(source, type);
      case SUMMARY:
        return summaryMetricToJson(source, type);
      case HISTOGRAM:
        return histogramMetricToJson(source, type);
      case EXPONENTIAL_HISTOGRAM:
        return exponentialHistogramMetricToJson(source, type);
    }
    throw new UnsupportedOperationException();
  }

  @Mapping(target = "gauge", ignore = true)
  protected abstract GaugeMetric gaugeMetricToJson(MetricData source, @Context MetricDataType type);

  @Mapping(target = "histogram", ignore = true)
  protected abstract HistogramMetric histogramMetricToJson(
      MetricData source, @Context MetricDataType type);

  @Mapping(target = "sum", ignore = true)
  protected abstract SumMetric sumMetricToJson(MetricData source, @Context MetricDataType type);

  @Mapping(target = "summary", ignore = true)
  protected abstract SummaryMetric summaryMetricToJson(
      MetricData source, @Context MetricDataType type);

  @Mapping(target = "exponentialHistogram", ignore = true)
  protected abstract ExponentialHistogramMetric exponentialHistogramMetricToJson(
      MetricData source, @Context MetricDataType type);

  // FROM JSON
  public MetricData jsonMetricToMetric(
      MetricDataJson source, @Context Resource resource, @Context InstrumentationScopeInfo scope) {
    DataJson<?> data = source.getData();
    if (data instanceof ExponentialHistogram) {
      return jsonMetricToMetric(source, resource, scope, MetricDataType.EXPONENTIAL_HISTOGRAM);
    } else if (data instanceof Histogram) {
      return jsonMetricToMetric(source, resource, scope, MetricDataType.HISTOGRAM);
    } else if (data instanceof Summary) {
      return jsonMetricToMetric(source, resource, scope, MetricDataType.SUMMARY);
    } else if (data instanceof Gauge) {
      List<NumberDataPoint> points = ((Gauge) data).getPoints();
      switch (getNumberDataPointType(points)) {
        case LONG:
          return jsonMetricToMetric(source, resource, scope, MetricDataType.LONG_GAUGE);
        case DOUBLE:
          return jsonMetricToMetric(source, resource, scope, MetricDataType.DOUBLE_GAUGE);
      }
    } else if (data instanceof Sum) {
      List<NumberDataPoint> points = ((Sum) data).getPoints();
      switch (getNumberDataPointType(points)) {
        case LONG:
          return jsonMetricToMetric(source, resource, scope, MetricDataType.LONG_SUM);
        case DOUBLE:
          return jsonMetricToMetric(source, resource, scope, MetricDataType.DOUBLE_SUM);
      }
    }

    throw new IllegalArgumentException();
  }

  @BeanMapping(resultType = MetricDataImpl.class)
  @Mapping(target = "resource", ignore = true)
  @Mapping(target = "instrumentationScopeInfo", ignore = true)
  @Mapping(target = "type", ignore = true)
  protected abstract MetricData jsonMetricToMetric(
      MetricDataJson source,
      @Context Resource resource,
      @Context InstrumentationScopeInfo scope,
      @Context MetricDataType type);

  private static NumberDataPoint.Type getNumberDataPointType(List<NumberDataPoint> points) {
    if (points == null || points.isEmpty()) {
      return NumberDataPoint.Type.LONG;
    }
    return points.get(0).getType();
  }

  @AfterMapping
  protected void addContextItems(
      @Context Resource resource,
      @Context InstrumentationScopeInfo scope,
      @Context MetricDataType type,
      @MappingTarget MetricDataImpl.Builder target) {
    target.setResource(resource);
    target.setInstrumentationScopeInfo(scope);
    target.setType(type);
  }
}
