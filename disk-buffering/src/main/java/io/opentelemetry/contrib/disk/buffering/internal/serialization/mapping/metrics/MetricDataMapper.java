/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.metrics;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.common.AttributesMapper;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.common.ByteStringMapper;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.metrics.v1.AggregationTemporality;
import io.opentelemetry.proto.metrics.v1.Exemplar;
import io.opentelemetry.proto.metrics.v1.ExponentialHistogram;
import io.opentelemetry.proto.metrics.v1.ExponentialHistogramDataPoint;
import io.opentelemetry.proto.metrics.v1.Gauge;
import io.opentelemetry.proto.metrics.v1.Histogram;
import io.opentelemetry.proto.metrics.v1.HistogramDataPoint;
import io.opentelemetry.proto.metrics.v1.Metric;
import io.opentelemetry.proto.metrics.v1.NumberDataPoint;
import io.opentelemetry.proto.metrics.v1.Sum;
import io.opentelemetry.proto.metrics.v1.Summary;
import io.opentelemetry.proto.metrics.v1.SummaryDataPoint;
import io.opentelemetry.sdk.common.InstrumentationScopeInfo;
import io.opentelemetry.sdk.metrics.data.Data;
import io.opentelemetry.sdk.metrics.data.DoubleExemplarData;
import io.opentelemetry.sdk.metrics.data.DoublePointData;
import io.opentelemetry.sdk.metrics.data.ExemplarData;
import io.opentelemetry.sdk.metrics.data.ExponentialHistogramBuckets;
import io.opentelemetry.sdk.metrics.data.ExponentialHistogramData;
import io.opentelemetry.sdk.metrics.data.ExponentialHistogramPointData;
import io.opentelemetry.sdk.metrics.data.GaugeData;
import io.opentelemetry.sdk.metrics.data.HistogramData;
import io.opentelemetry.sdk.metrics.data.HistogramPointData;
import io.opentelemetry.sdk.metrics.data.LongExemplarData;
import io.opentelemetry.sdk.metrics.data.LongPointData;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.data.MetricDataType;
import io.opentelemetry.sdk.metrics.data.PointData;
import io.opentelemetry.sdk.metrics.data.SumData;
import io.opentelemetry.sdk.metrics.data.SummaryData;
import io.opentelemetry.sdk.metrics.data.SummaryPointData;
import io.opentelemetry.sdk.metrics.data.ValueAtQuantile;
import io.opentelemetry.sdk.metrics.internal.data.ImmutableDoubleExemplarData;
import io.opentelemetry.sdk.metrics.internal.data.ImmutableDoublePointData;
import io.opentelemetry.sdk.metrics.internal.data.ImmutableExponentialHistogramBuckets;
import io.opentelemetry.sdk.metrics.internal.data.ImmutableExponentialHistogramData;
import io.opentelemetry.sdk.metrics.internal.data.ImmutableExponentialHistogramPointData;
import io.opentelemetry.sdk.metrics.internal.data.ImmutableGaugeData;
import io.opentelemetry.sdk.metrics.internal.data.ImmutableHistogramData;
import io.opentelemetry.sdk.metrics.internal.data.ImmutableHistogramPointData;
import io.opentelemetry.sdk.metrics.internal.data.ImmutableLongExemplarData;
import io.opentelemetry.sdk.metrics.internal.data.ImmutableLongPointData;
import io.opentelemetry.sdk.metrics.internal.data.ImmutableMetricData;
import io.opentelemetry.sdk.metrics.internal.data.ImmutableSumData;
import io.opentelemetry.sdk.metrics.internal.data.ImmutableSummaryData;
import io.opentelemetry.sdk.metrics.internal.data.ImmutableSummaryPointData;
import io.opentelemetry.sdk.metrics.internal.data.ImmutableValueAtQuantile;
import io.opentelemetry.sdk.resources.Resource;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public class MetricDataMapper {

  private static final MetricDataMapper INSTANCE = new MetricDataMapper();

  public static MetricDataMapper getInstance() {
    return INSTANCE;
  }

  public Metric mapToProto(MetricData source) {
    Metric.Builder metric = new Metric.Builder();

    metric.name(source.getName());
    metric.description(source.getDescription());
    metric.unit(source.getUnit());

    addDataToProto(source, metric);

    return metric.build();
  }

  @SuppressWarnings("unchecked")
  public MetricData mapToSdk(Metric source, Resource resource, InstrumentationScopeInfo scope) {
    if (source.gauge != null) {
      DataWithType gaugeDataWithType = mapGaugeToSdk(source.gauge);
      if (gaugeDataWithType.type == MetricDataType.DOUBLE_GAUGE) {
        return ImmutableMetricData.createDoubleGauge(
            resource,
            scope,
            source.name,
            source.description,
            source.unit,
            (GaugeData<DoublePointData>) gaugeDataWithType.data);
      } else {
        return ImmutableMetricData.createLongGauge(
            resource,
            scope,
            source.name,
            source.description,
            source.unit,
            (GaugeData<LongPointData>) gaugeDataWithType.data);
      }
    } else if (source.sum != null) {
      DataWithType sumDataWithType = mapSumToSdk(source.sum);
      if (sumDataWithType.type == MetricDataType.DOUBLE_SUM) {
        return ImmutableMetricData.createDoubleSum(
            resource,
            scope,
            source.name,
            source.description,
            source.unit,
            (SumData<DoublePointData>) sumDataWithType.data);
      } else {
        return ImmutableMetricData.createLongSum(
            resource,
            scope,
            source.name,
            source.description,
            source.unit,
            (SumData<LongPointData>) sumDataWithType.data);
      }
    } else if (source.summary != null) {
      return ImmutableMetricData.createDoubleSummary(
          resource,
          scope,
          source.name,
          source.description,
          source.unit,
          mapSummaryToSdk(source.summary));
    } else if (source.histogram != null) {
      return ImmutableMetricData.createDoubleHistogram(
          resource,
          scope,
          source.name,
          source.description,
          source.unit,
          mapHistogramToSdk(source.histogram));
    } else if (source.exponential_histogram != null) {
      return ImmutableMetricData.createExponentialHistogram(
          resource,
          scope,
          source.name,
          source.description,
          source.unit,
          mapExponentialHistogramToSdk(source.exponential_histogram));
    } else {
      throw new UnsupportedOperationException();
    }
  }

  @SuppressWarnings("unchecked")
  private static void addDataToProto(MetricData source, Metric.Builder target) {
    switch (source.getType()) {
      case LONG_GAUGE:
        target.gauge(mapLongGaugeToProto((GaugeData<LongPointData>) source.getData()));
        break;
      case DOUBLE_GAUGE:
        target.gauge(mapDoubleGaugeToProto((GaugeData<DoublePointData>) source.getData()));
        break;
      case LONG_SUM:
        target.sum(mapLongSumToProto((SumData<LongPointData>) source.getData()));
        break;
      case DOUBLE_SUM:
        target.sum(mapDoubleSumToProto((SumData<DoublePointData>) source.getData()));
        break;
      case SUMMARY:
        target.summary(mapSummaryToProto((SummaryData) source.getData()));
        break;
      case HISTOGRAM:
        target.histogram(mapHistogramToProto((HistogramData) source.getData()));
        break;
      case EXPONENTIAL_HISTOGRAM:
        target.exponential_histogram(
            mapExponentialHistogramToProto((ExponentialHistogramData) source.getData()));
        break;
    }
  }

  private static DataWithType mapGaugeToSdk(Gauge gauge) {
    if (!gauge.data_points.isEmpty()) {
      NumberDataPoint dataPoint = gauge.data_points.get(0);
      if (dataPoint.as_int != null) {
        return new DataWithType(mapLongGaugeToSdk(gauge), MetricDataType.LONG_GAUGE);
      } else if (dataPoint.as_double != null) {
        return new DataWithType(mapDoubleGaugeToSdk(gauge), MetricDataType.DOUBLE_GAUGE);
      }
    }
    return new DataWithType(mapDoubleGaugeToSdk(gauge), MetricDataType.DOUBLE_GAUGE);
  }

  private static DataWithType mapSumToSdk(Sum sum) {
    if (!sum.data_points.isEmpty()) {
      NumberDataPoint dataPoint = sum.data_points.get(0);
      if (dataPoint.as_int != null) {
        return new DataWithType(mapLongSumToSdk(sum), MetricDataType.LONG_SUM);
      } else if (dataPoint.as_double != null) {
        return new DataWithType(mapDoubleSumToSdk(sum), MetricDataType.DOUBLE_SUM);
      }
    }
    return new DataWithType(mapDoubleSumToSdk(sum), MetricDataType.DOUBLE_SUM);
  }

  private static Gauge mapLongGaugeToProto(GaugeData<LongPointData> data) {
    Gauge.Builder gauge = new Gauge.Builder();

    if (data.getPoints() != null) {
      for (LongPointData point : data.getPoints()) {
        gauge.data_points.add(longPointDataToNumberDataPoint(point));
      }
    }

    return gauge.build();
  }

  private static Gauge mapDoubleGaugeToProto(GaugeData<DoublePointData> data) {
    Gauge.Builder gauge = new Gauge.Builder();

    if (data.getPoints() != null) {
      for (DoublePointData point : data.getPoints()) {
        gauge.data_points.add(doublePointDataToNumberDataPoint(point));
      }
    }

    return gauge.build();
  }

  private static Sum mapLongSumToProto(SumData<LongPointData> data) {
    Sum.Builder sum = new Sum.Builder();

    if (data.getPoints() != null) {
      for (LongPointData point : data.getPoints()) {
        sum.data_points.add(longPointDataToNumberDataPoint(point));
      }
    }
    sum.is_monotonic(data.isMonotonic());
    sum.aggregation_temporality(mapAggregationTemporalityToProto(data.getAggregationTemporality()));

    return sum.build();
  }

  private static Sum mapDoubleSumToProto(SumData<DoublePointData> data) {
    Sum.Builder sum = new Sum.Builder();

    if (data.getPoints() != null) {
      for (DoublePointData point : data.getPoints()) {
        sum.data_points.add(doublePointDataToNumberDataPoint(point));
      }
    }
    sum.is_monotonic(data.isMonotonic());
    sum.aggregation_temporality(mapAggregationTemporalityToProto(data.getAggregationTemporality()));

    return sum.build();
  }

  private static Summary mapSummaryToProto(SummaryData data) {
    Summary.Builder summary = new Summary.Builder();

    if (data.getPoints() != null) {
      for (SummaryPointData point : data.getPoints()) {
        summary.data_points.add(summaryPointDataToSummaryDataPoint(point));
      }
    }

    return summary.build();
  }

  private static Histogram mapHistogramToProto(HistogramData data) {
    Histogram.Builder histogram = new Histogram.Builder();

    if (data.getPoints() != null) {
      for (HistogramPointData point : data.getPoints()) {
        histogram.data_points.add(histogramPointDataToHistogramDataPoint(point));
      }
    }
    histogram.aggregation_temporality(
        mapAggregationTemporalityToProto(data.getAggregationTemporality()));

    return histogram.build();
  }

  private static ExponentialHistogram mapExponentialHistogramToProto(
      ExponentialHistogramData data) {
    ExponentialHistogram.Builder exponentialHistogram = new ExponentialHistogram.Builder();

    if (data.getPoints() != null) {
      for (ExponentialHistogramPointData point : data.getPoints()) {
        exponentialHistogram.data_points.add(
            exponentialHistogramPointDataToExponentialHistogramDataPoint(point));
      }
    }
    exponentialHistogram.aggregation_temporality(
        mapAggregationTemporalityToProto(data.getAggregationTemporality()));

    return exponentialHistogram.build();
  }

  private static NumberDataPoint longPointDataToNumberDataPoint(LongPointData source) {
    NumberDataPoint.Builder numberDataPoint = new NumberDataPoint.Builder();

    numberDataPoint.start_time_unix_nano(source.getStartEpochNanos());
    numberDataPoint.time_unix_nano(source.getEpochNanos());
    numberDataPoint.as_int(source.getValue());
    if (source.getExemplars() != null) {
      for (LongExemplarData exemplar : source.getExemplars()) {
        numberDataPoint.exemplars.add(longExemplarDataToExemplar(exemplar));
      }
    }

    addAttributesToNumberDataPoint(source, numberDataPoint);

    return numberDataPoint.build();
  }

  private static void addAttributesToNumberDataPoint(
      PointData source, NumberDataPoint.Builder target) {
    target.attributes.addAll(attributesToProto(source.getAttributes()));
  }

  private static NumberDataPoint doublePointDataToNumberDataPoint(DoublePointData source) {
    NumberDataPoint.Builder numberDataPoint = new NumberDataPoint.Builder();

    numberDataPoint.start_time_unix_nano(source.getStartEpochNanos());
    numberDataPoint.time_unix_nano(source.getEpochNanos());
    numberDataPoint.as_double(source.getValue());
    if (source.getExemplars() != null) {
      for (DoubleExemplarData exemplar : source.getExemplars()) {
        numberDataPoint.exemplars.add(doubleExemplarDataToExemplar(exemplar));
      }
    }

    addAttributesToNumberDataPoint(source, numberDataPoint);

    return numberDataPoint.build();
  }

  private static SummaryDataPoint summaryPointDataToSummaryDataPoint(
      SummaryPointData summaryPointData) {
    SummaryDataPoint.Builder summaryDataPoint = new SummaryDataPoint.Builder();

    summaryDataPoint.start_time_unix_nano(summaryPointData.getStartEpochNanos());
    summaryDataPoint.time_unix_nano(summaryPointData.getEpochNanos());
    if (summaryPointData.getValues() != null) {
      for (ValueAtQuantile value : summaryPointData.getValues()) {
        summaryDataPoint.quantile_values.add(valueAtQuantileToValueAtQuantile(value));
      }
    }
    summaryDataPoint.count(summaryPointData.getCount());
    summaryDataPoint.sum(summaryPointData.getSum());

    addAttributesToSummaryDataPoint(summaryPointData, summaryDataPoint);

    return summaryDataPoint.build();
  }

  private static void addAttributesToSummaryDataPoint(
      PointData source, SummaryDataPoint.Builder target) {
    target.attributes.addAll(attributesToProto(source.getAttributes()));
  }

  private static HistogramDataPoint histogramPointDataToHistogramDataPoint(
      HistogramPointData histogramPointData) {
    HistogramDataPoint.Builder histogramDataPoint = new HistogramDataPoint.Builder();

    histogramDataPoint.start_time_unix_nano(histogramPointData.getStartEpochNanos());
    histogramDataPoint.time_unix_nano(histogramPointData.getEpochNanos());
    if (histogramPointData.getCounts() != null) {
      histogramDataPoint.bucket_counts.addAll(histogramPointData.getCounts());
    }
    if (histogramPointData.getBoundaries() != null) {
      histogramDataPoint.explicit_bounds.addAll(histogramPointData.getBoundaries());
    }
    if (histogramPointData.getExemplars() != null) {
      for (DoubleExemplarData exemplar : histogramPointData.getExemplars()) {
        histogramDataPoint.exemplars.add(doubleExemplarDataToExemplar(exemplar));
      }
    }
    histogramDataPoint.count(histogramPointData.getCount());
    histogramDataPoint.sum(histogramPointData.getSum());
    if (histogramPointData.hasMin()) {
      histogramDataPoint.min(histogramPointData.getMin());
    }
    if (histogramPointData.hasMax()) {
      histogramDataPoint.max(histogramPointData.getMax());
    }

    addAttributesToHistogramDataPoint(histogramPointData, histogramDataPoint);

    return histogramDataPoint.build();
  }

  private static void addAttributesToHistogramDataPoint(
      HistogramPointData source, HistogramDataPoint.Builder target) {
    target.attributes.addAll(attributesToProto(source.getAttributes()));
  }

  private static ExponentialHistogramDataPoint
      exponentialHistogramPointDataToExponentialHistogramDataPoint(
          ExponentialHistogramPointData exponentialHistogramPointData) {
    ExponentialHistogramDataPoint.Builder exponentialHistogramDataPoint =
        new ExponentialHistogramDataPoint.Builder();

    exponentialHistogramDataPoint.start_time_unix_nano(
        exponentialHistogramPointData.getStartEpochNanos());
    exponentialHistogramDataPoint.time_unix_nano(exponentialHistogramPointData.getEpochNanos());
    exponentialHistogramDataPoint.positive(
        exponentialHistogramBucketsToBuckets(exponentialHistogramPointData.getPositiveBuckets()));
    exponentialHistogramDataPoint.negative(
        exponentialHistogramBucketsToBuckets(exponentialHistogramPointData.getNegativeBuckets()));
    if (exponentialHistogramPointData.getExemplars() != null) {
      for (DoubleExemplarData exemplar : exponentialHistogramPointData.getExemplars()) {
        exponentialHistogramDataPoint.exemplars.add(doubleExemplarDataToExemplar(exemplar));
      }
    }
    exponentialHistogramDataPoint.count(exponentialHistogramPointData.getCount());
    exponentialHistogramDataPoint.sum(exponentialHistogramPointData.getSum());
    exponentialHistogramDataPoint.scale(exponentialHistogramPointData.getScale());
    exponentialHistogramDataPoint.zero_count(exponentialHistogramPointData.getZeroCount());
    if (exponentialHistogramPointData.hasMin()) {
      exponentialHistogramDataPoint.min(exponentialHistogramPointData.getMin());
    }
    if (exponentialHistogramPointData.hasMax()) {
      exponentialHistogramDataPoint.max(exponentialHistogramPointData.getMax());
    }

    addAttributesToExponentialHistogramDataPoint(
        exponentialHistogramPointData, exponentialHistogramDataPoint);

    return exponentialHistogramDataPoint.build();
  }

  private static void addAttributesToExponentialHistogramDataPoint(
      ExponentialHistogramPointData source, ExponentialHistogramDataPoint.Builder target) {
    target.attributes.addAll(attributesToProto(source.getAttributes()));
  }

  private static ExponentialHistogramDataPoint.Buckets exponentialHistogramBucketsToBuckets(
      ExponentialHistogramBuckets source) {
    ExponentialHistogramDataPoint.Buckets.Builder buckets =
        new ExponentialHistogramDataPoint.Buckets.Builder();

    if (source.getBucketCounts() != null) {
      buckets.bucket_counts.addAll(source.getBucketCounts());
    }
    buckets.offset(source.getOffset());

    return buckets.build();
  }

  private static Exemplar doubleExemplarDataToExemplar(DoubleExemplarData doubleExemplarData) {
    Exemplar.Builder exemplar = new Exemplar.Builder();

    exemplar.time_unix_nano(doubleExemplarData.getEpochNanos());
    exemplar.as_double(doubleExemplarData.getValue());

    addExtrasToExemplar(doubleExemplarData, exemplar);

    return exemplar.build();
  }

  private static Exemplar longExemplarDataToExemplar(LongExemplarData doubleExemplarData) {
    Exemplar.Builder exemplar = new Exemplar.Builder();

    exemplar.time_unix_nano(doubleExemplarData.getEpochNanos());
    exemplar.as_int(doubleExemplarData.getValue());

    addExtrasToExemplar(doubleExemplarData, exemplar);

    return exemplar.build();
  }

  private static void addExtrasToExemplar(ExemplarData source, Exemplar.Builder target) {
    target.filtered_attributes.addAll(attributesToProto(source.getFilteredAttributes()));
    SpanContext spanContext = source.getSpanContext();
    target.span_id(ByteStringMapper.getInstance().stringToProto(spanContext.getSpanId()));
    target.trace_id(ByteStringMapper.getInstance().stringToProto(spanContext.getTraceId()));
  }

  private static AggregationTemporality mapAggregationTemporalityToProto(
      io.opentelemetry.sdk.metrics.data.AggregationTemporality source) {
    AggregationTemporality aggregationTemporality;

    switch (source) {
      case DELTA:
        aggregationTemporality = AggregationTemporality.AGGREGATION_TEMPORALITY_DELTA;
        break;
      case CUMULATIVE:
        aggregationTemporality = AggregationTemporality.AGGREGATION_TEMPORALITY_CUMULATIVE;
        break;
      default:
        aggregationTemporality = AggregationTemporality.AGGREGATION_TEMPORALITY_UNSPECIFIED;
    }

    return aggregationTemporality;
  }

  private static SummaryData mapSummaryToSdk(Summary summary) {
    return ImmutableSummaryData.create(
        summaryDataPointListToSummaryPointDataCollection(summary.data_points));
  }

  private static HistogramData mapHistogramToSdk(Histogram histogram) {
    return ImmutableHistogramData.create(
        mapAggregationTemporalityToSdk(histogram.aggregation_temporality),
        histogramDataPointListToHistogramPointDataCollection(histogram.data_points));
  }

  private static ExponentialHistogramData mapExponentialHistogramToSdk(
      ExponentialHistogram source) {
    return ImmutableExponentialHistogramData.create(
        mapAggregationTemporalityToSdk(source.aggregation_temporality),
        exponentialHistogramDataPointListToExponentialHistogramPointDataCollection(
            source.data_points));
  }

  private static ExponentialHistogramPointData
      exponentialHistogramDataPointToExponentialHistogramPointData(
          ExponentialHistogramDataPoint source) {
    double min = (source.min != null) ? source.min : 0;
    double max = (source.max != null) ? source.max : 0;
    return ImmutableExponentialHistogramPointData.create(
        source.scale,
        source.sum,
        source.zero_count,
        min > 0,
        min,
        max > 0,
        max,
        mapBucketsFromProto(source.positive, source.scale),
        mapBucketsFromProto(source.negative, source.scale),
        source.start_time_unix_nano,
        source.time_unix_nano,
        protoToAttributes(source.attributes),
        exemplarListToDoubleExemplarDataList(source.exemplars));
  }

  private static HistogramPointData histogramDataPointToHistogramPointData(
      HistogramDataPoint source) {
    double min = (source.min != null) ? source.min : 0;
    double max = (source.max != null) ? source.max : 0;
    return ImmutableHistogramPointData.create(
        source.start_time_unix_nano,
        source.time_unix_nano,
        protoToAttributes(source.attributes),
        source.sum,
        min > 0,
        min,
        max > 0,
        max,
        source.explicit_bounds,
        source.bucket_counts,
        exemplarListToDoubleExemplarDataList(source.exemplars));
  }

  @NotNull
  private static DoubleExemplarData exemplarToDoubleExemplarData(Exemplar source) {
    return ImmutableDoubleExemplarData.create(
        protoToAttributes(source.filtered_attributes),
        source.time_unix_nano,
        createSpanContext(source),
        source.as_double);
  }

  @NotNull
  private static LongExemplarData exemplarToLongExemplarData(Exemplar source) {
    return ImmutableLongExemplarData.create(
        protoToAttributes(source.filtered_attributes),
        source.time_unix_nano,
        createSpanContext(source),
        source.as_int);
  }

  @NotNull
  private static SpanContext createSpanContext(Exemplar value) {
    return SpanContext.create(
        ByteStringMapper.getInstance().protoToString(value.trace_id),
        ByteStringMapper.getInstance().protoToString(value.span_id),
        TraceFlags.getSampled(),
        TraceState.getDefault());
  }

  private static SummaryPointData summaryDataPointToSummaryPointData(SummaryDataPoint source) {
    return ImmutableSummaryPointData.create(
        source.start_time_unix_nano,
        source.time_unix_nano,
        protoToAttributes(source.attributes),
        source.count,
        source.sum,
        valueAtQuantileListToValueAtQuantileList(source.quantile_values));
  }

  private static ValueAtQuantile mapFromSummaryValueAtQuantileProto(
      SummaryDataPoint.ValueAtQuantile source) {
    return ImmutableValueAtQuantile.create(source.quantile, source.value);
  }

  private static io.opentelemetry.sdk.metrics.data.AggregationTemporality
      mapAggregationTemporalityToSdk(AggregationTemporality source) {
    io.opentelemetry.sdk.metrics.data.AggregationTemporality aggregationTemporality;

    switch (source) {
      case AGGREGATION_TEMPORALITY_DELTA:
        aggregationTemporality = io.opentelemetry.sdk.metrics.data.AggregationTemporality.DELTA;
        break;
      case AGGREGATION_TEMPORALITY_CUMULATIVE:
        aggregationTemporality =
            io.opentelemetry.sdk.metrics.data.AggregationTemporality.CUMULATIVE;
        break;
      default:
        throw new IllegalArgumentException("Unexpected enum constant: " + source);
    }

    return aggregationTemporality;
  }

  private static GaugeData<LongPointData> mapLongGaugeToSdk(Gauge gauge) {
    return ImmutableGaugeData.create(
        numberDataPointListToLongPointDataCollection(gauge.data_points));
  }

  private static GaugeData<DoublePointData> mapDoubleGaugeToSdk(Gauge gauge) {
    return ImmutableGaugeData.create(
        numberDataPointListToDoublePointDataCollection(gauge.data_points));
  }

  private static SumData<LongPointData> mapLongSumToSdk(Sum sum) {
    return ImmutableSumData.create(
        sum.is_monotonic,
        mapAggregationTemporalityToSdk(sum.aggregation_temporality),
        numberDataPointListToLongPointDataCollection(sum.data_points));
  }

  private static SumData<DoublePointData> mapDoubleSumToSdk(Sum sum) {
    return ImmutableSumData.create(
        sum.is_monotonic,
        mapAggregationTemporalityToSdk(sum.aggregation_temporality),
        numberDataPointListToDoublePointDataCollection(sum.data_points));
  }

  private static DoublePointData mapDoubleNumberDataPointToSdk(NumberDataPoint source) {
    return ImmutableDoublePointData.create(
        source.start_time_unix_nano,
        source.time_unix_nano,
        protoToAttributes(source.attributes),
        source.as_double,
        exemplarListToDoubleExemplarDataList(source.exemplars));
  }

  private static LongPointData mapLongNumberDataPointToSdk(NumberDataPoint source) {
    return ImmutableLongPointData.create(
        source.start_time_unix_nano,
        source.time_unix_nano,
        protoToAttributes(source.attributes),
        source.as_int,
        exemplarListToLongExemplarDataList(source.exemplars));
  }

  private static SummaryDataPoint.ValueAtQuantile valueAtQuantileToValueAtQuantile(
      ValueAtQuantile valueAtQuantile) {
    SummaryDataPoint.ValueAtQuantile.Builder builder =
        new SummaryDataPoint.ValueAtQuantile.Builder();

    builder.quantile(valueAtQuantile.getQuantile());
    builder.value(valueAtQuantile.getValue());

    return builder.build();
  }

  private static List<SummaryPointData> summaryDataPointListToSummaryPointDataCollection(
      List<SummaryDataPoint> list) {
    List<SummaryPointData> collection = new ArrayList<>(list.size());
    for (SummaryDataPoint summaryDataPoint : list) {
      collection.add(summaryDataPointToSummaryPointData(summaryDataPoint));
    }

    return collection;
  }

  private static List<HistogramPointData> histogramDataPointListToHistogramPointDataCollection(
      List<HistogramDataPoint> list) {
    List<HistogramPointData> collection = new ArrayList<>(list.size());
    for (HistogramDataPoint histogramDataPoint : list) {
      collection.add(histogramDataPointToHistogramPointData(histogramDataPoint));
    }

    return collection;
  }

  private static List<ExponentialHistogramPointData>
      exponentialHistogramDataPointListToExponentialHistogramPointDataCollection(
          List<ExponentialHistogramDataPoint> list) {
    List<ExponentialHistogramPointData> collection = new ArrayList<>(list.size());
    for (ExponentialHistogramDataPoint exponentialHistogramDataPoint : list) {
      collection.add(
          exponentialHistogramDataPointToExponentialHistogramPointData(
              exponentialHistogramDataPoint));
    }

    return collection;
  }

  private static List<DoubleExemplarData> exemplarListToDoubleExemplarDataList(
      List<Exemplar> list) {
    List<DoubleExemplarData> result = new ArrayList<>(list.size());
    for (Exemplar exemplar : list) {
      result.add(exemplarToDoubleExemplarData(exemplar));
    }

    return result;
  }

  private static List<ValueAtQuantile> valueAtQuantileListToValueAtQuantileList(
      List<SummaryDataPoint.ValueAtQuantile> list) {
    List<ValueAtQuantile> result = new ArrayList<>(list.size());
    for (SummaryDataPoint.ValueAtQuantile valueAtQuantile : list) {
      result.add(mapFromSummaryValueAtQuantileProto(valueAtQuantile));
    }

    return result;
  }

  private static List<LongPointData> numberDataPointListToLongPointDataCollection(
      List<NumberDataPoint> list) {
    List<LongPointData> collection = new ArrayList<>(list.size());
    for (NumberDataPoint numberDataPoint : list) {
      collection.add(mapLongNumberDataPointToSdk(numberDataPoint));
    }

    return collection;
  }

  private static List<DoublePointData> numberDataPointListToDoublePointDataCollection(
      List<NumberDataPoint> list) {
    List<DoublePointData> collection = new ArrayList<>(list.size());
    for (NumberDataPoint numberDataPoint : list) {
      collection.add(mapDoubleNumberDataPointToSdk(numberDataPoint));
    }

    return collection;
  }

  private static List<LongExemplarData> exemplarListToLongExemplarDataList(List<Exemplar> list) {
    List<LongExemplarData> result = new ArrayList<>(list.size());
    for (Exemplar exemplar : list) {
      result.add(exemplarToLongExemplarData(exemplar));
    }

    return result;
  }

  private static ExponentialHistogramBuckets mapBucketsFromProto(
      ExponentialHistogramDataPoint.Buckets source, int scale) {
    return ImmutableExponentialHistogramBuckets.create(scale, source.offset, source.bucket_counts);
  }

  private static List<KeyValue> attributesToProto(Attributes source) {
    return AttributesMapper.getInstance().attributesToProto(source);
  }

  private static Attributes protoToAttributes(List<KeyValue> source) {
    return AttributesMapper.getInstance().protoToAttributes(source);
  }

  private static final class DataWithType {
    final Data<?> data;
    final MetricDataType type;

    private DataWithType(Data<?> data, MetricDataType type) {
      this.data = data;
      this.type = type;
    }
  }
}
