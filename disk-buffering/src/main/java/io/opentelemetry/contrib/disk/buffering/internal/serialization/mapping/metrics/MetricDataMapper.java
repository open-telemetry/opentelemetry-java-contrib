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

public final class MetricDataMapper {

  private static final MetricDataMapper INSTANCE = new MetricDataMapper();

  public static MetricDataMapper getInstance() {
    return INSTANCE;
  }

  public Metric mapToProto(MetricData source) {
    Metric.Builder metric = Metric.newBuilder();

    metric.setName(source.getName());
    metric.setDescription(source.getDescription());
    metric.setUnit(source.getUnit());

    addDataToProto(source, metric);

    return metric.build();
  }

  @SuppressWarnings("unchecked")
  public MetricData mapToSdk(Metric source, Resource resource, InstrumentationScopeInfo scope) {
    switch (source.getDataCase()) {
      case GAUGE:
        DataWithType gaugeDataWithType = mapGaugeToSdk(source.getGauge());
        if (gaugeDataWithType.type == MetricDataType.DOUBLE_GAUGE) {
          return ImmutableMetricData.createDoubleGauge(
              resource,
              scope,
              source.getName(),
              source.getDescription(),
              source.getUnit(),
              (GaugeData<DoublePointData>) gaugeDataWithType.data);
        } else {
          return ImmutableMetricData.createLongGauge(
              resource,
              scope,
              source.getName(),
              source.getDescription(),
              source.getUnit(),
              (GaugeData<LongPointData>) gaugeDataWithType.data);
        }
      case SUM:
        DataWithType sumDataWithType = mapSumToSdk(source.getSum());
        if (sumDataWithType.type == MetricDataType.DOUBLE_SUM) {
          return ImmutableMetricData.createDoubleSum(
              resource,
              scope,
              source.getName(),
              source.getDescription(),
              source.getUnit(),
              (SumData<DoublePointData>) sumDataWithType.data);
        } else {
          return ImmutableMetricData.createLongSum(
              resource,
              scope,
              source.getName(),
              source.getDescription(),
              source.getUnit(),
              (SumData<LongPointData>) sumDataWithType.data);
        }
      case SUMMARY:
        return ImmutableMetricData.createDoubleSummary(
            resource,
            scope,
            source.getName(),
            source.getDescription(),
            source.getUnit(),
            mapSummaryToSdk(source.getSummary()));
      case HISTOGRAM:
        return ImmutableMetricData.createDoubleHistogram(
            resource,
            scope,
            source.getName(),
            source.getDescription(),
            source.getUnit(),
            mapHistogramToSdk(source.getHistogram()));
      case EXPONENTIAL_HISTOGRAM:
        return ImmutableMetricData.createExponentialHistogram(
            resource,
            scope,
            source.getName(),
            source.getDescription(),
            source.getUnit(),
            mapExponentialHistogramToSdk(source.getExponentialHistogram()));
      default:
        throw new UnsupportedOperationException();
    }
  }

  @SuppressWarnings("unchecked")
  private static void addDataToProto(MetricData source, Metric.Builder target) {
    switch (source.getType()) {
      case LONG_GAUGE:
        target.setGauge(mapLongGaugeToProto((GaugeData<LongPointData>) source.getData()));
        break;
      case DOUBLE_GAUGE:
        target.setGauge(mapDoubleGaugeToProto((GaugeData<DoublePointData>) source.getData()));
        break;
      case LONG_SUM:
        target.setSum(mapLongSumToProto((SumData<LongPointData>) source.getData()));
        break;
      case DOUBLE_SUM:
        target.setSum(mapDoubleSumToProto((SumData<DoublePointData>) source.getData()));
        break;
      case SUMMARY:
        target.setSummary(mapSummaryToProto((SummaryData) source.getData()));
        break;
      case HISTOGRAM:
        target.setHistogram(mapHistogramToProto((HistogramData) source.getData()));
        break;
      case EXPONENTIAL_HISTOGRAM:
        target.setExponentialHistogram(
            mapExponentialHistogramToProto((ExponentialHistogramData) source.getData()));
        break;
    }
  }

  private static DataWithType mapGaugeToSdk(Gauge gauge) {
    if (gauge.getDataPointsCount() > 0) {
      NumberDataPoint dataPoint = gauge.getDataPoints(0);
      if (dataPoint.hasAsInt()) {
        return new DataWithType(mapLongGaugeToSdk(gauge), MetricDataType.LONG_GAUGE);
      } else if (dataPoint.hasAsDouble()) {
        return new DataWithType(mapDoubleGaugeToSdk(gauge), MetricDataType.DOUBLE_GAUGE);
      }
    }
    return new DataWithType(mapDoubleGaugeToSdk(gauge), MetricDataType.DOUBLE_GAUGE);
  }

  private static DataWithType mapSumToSdk(Sum sum) {
    if (sum.getDataPointsCount() > 0) {
      NumberDataPoint dataPoint = sum.getDataPoints(0);
      if (dataPoint.hasAsInt()) {
        return new DataWithType(mapLongSumToSdk(sum), MetricDataType.LONG_SUM);
      } else if (dataPoint.hasAsDouble()) {
        return new DataWithType(mapDoubleSumToSdk(sum), MetricDataType.DOUBLE_SUM);
      }
    }
    return new DataWithType(mapDoubleSumToSdk(sum), MetricDataType.DOUBLE_SUM);
  }

  private static Gauge mapLongGaugeToProto(GaugeData<LongPointData> data) {
    Gauge.Builder gauge = Gauge.newBuilder();

    if (data.getPoints() != null) {
      for (LongPointData point : data.getPoints()) {
        gauge.addDataPoints(longPointDataToNumberDataPoint(point));
      }
    }

    return gauge.build();
  }

  private static Gauge mapDoubleGaugeToProto(GaugeData<DoublePointData> data) {
    Gauge.Builder gauge = Gauge.newBuilder();

    if (data.getPoints() != null) {
      for (DoublePointData point : data.getPoints()) {
        gauge.addDataPoints(doublePointDataToNumberDataPoint(point));
      }
    }

    return gauge.build();
  }

  private static Sum mapLongSumToProto(SumData<LongPointData> data) {
    Sum.Builder sum = Sum.newBuilder();

    if (data.getPoints() != null) {
      for (LongPointData point : data.getPoints()) {
        sum.addDataPoints(longPointDataToNumberDataPoint(point));
      }
    }
    sum.setIsMonotonic(data.isMonotonic());
    sum.setAggregationTemporality(
        mapAggregationTemporalityToProto(data.getAggregationTemporality()));

    return sum.build();
  }

  private static Sum mapDoubleSumToProto(SumData<DoublePointData> data) {
    Sum.Builder sum = Sum.newBuilder();

    if (data.getPoints() != null) {
      for (DoublePointData point : data.getPoints()) {
        sum.addDataPoints(doublePointDataToNumberDataPoint(point));
      }
    }
    sum.setIsMonotonic(data.isMonotonic());
    sum.setAggregationTemporality(
        mapAggregationTemporalityToProto(data.getAggregationTemporality()));

    return sum.build();
  }

  private static Summary mapSummaryToProto(SummaryData data) {
    Summary.Builder summary = Summary.newBuilder();

    if (data.getPoints() != null) {
      for (SummaryPointData point : data.getPoints()) {
        summary.addDataPoints(summaryPointDataToSummaryDataPoint(point));
      }
    }

    return summary.build();
  }

  private static Histogram mapHistogramToProto(HistogramData data) {
    Histogram.Builder histogram = Histogram.newBuilder();

    if (data.getPoints() != null) {
      for (HistogramPointData point : data.getPoints()) {
        histogram.addDataPoints(histogramPointDataToHistogramDataPoint(point));
      }
    }
    histogram.setAggregationTemporality(
        mapAggregationTemporalityToProto(data.getAggregationTemporality()));

    return histogram.build();
  }

  private static ExponentialHistogram mapExponentialHistogramToProto(
      ExponentialHistogramData data) {
    ExponentialHistogram.Builder exponentialHistogram = ExponentialHistogram.newBuilder();

    if (data.getPoints() != null) {
      for (ExponentialHistogramPointData point : data.getPoints()) {
        exponentialHistogram.addDataPoints(
            exponentialHistogramPointDataToExponentialHistogramDataPoint(point));
      }
    }
    exponentialHistogram.setAggregationTemporality(
        mapAggregationTemporalityToProto(data.getAggregationTemporality()));

    return exponentialHistogram.build();
  }

  private static NumberDataPoint longPointDataToNumberDataPoint(LongPointData source) {
    NumberDataPoint.Builder numberDataPoint = NumberDataPoint.newBuilder();

    numberDataPoint.setStartTimeUnixNano(source.getStartEpochNanos());
    numberDataPoint.setTimeUnixNano(source.getEpochNanos());
    numberDataPoint.setAsInt(source.getValue());
    if (source.getExemplars() != null) {
      for (LongExemplarData exemplar : source.getExemplars()) {
        numberDataPoint.addExemplars(longExemplarDataToExemplar(exemplar));
      }
    }

    addAttributesToNumberDataPoint(source, numberDataPoint);

    return numberDataPoint.build();
  }

  private static void addAttributesToNumberDataPoint(
      PointData source, NumberDataPoint.Builder target) {
    target.addAllAttributes(attributesToProto(source.getAttributes()));
  }

  private static NumberDataPoint doublePointDataToNumberDataPoint(DoublePointData source) {
    NumberDataPoint.Builder numberDataPoint = NumberDataPoint.newBuilder();

    numberDataPoint.setStartTimeUnixNano(source.getStartEpochNanos());
    numberDataPoint.setTimeUnixNano(source.getEpochNanos());
    numberDataPoint.setAsDouble(source.getValue());
    if (source.getExemplars() != null) {
      for (DoubleExemplarData exemplar : source.getExemplars()) {
        numberDataPoint.addExemplars(doubleExemplarDataToExemplar(exemplar));
      }
    }

    addAttributesToNumberDataPoint(source, numberDataPoint);

    return numberDataPoint.build();
  }

  private static SummaryDataPoint summaryPointDataToSummaryDataPoint(
      SummaryPointData summaryPointData) {
    SummaryDataPoint.Builder summaryDataPoint = SummaryDataPoint.newBuilder();

    summaryDataPoint.setStartTimeUnixNano(summaryPointData.getStartEpochNanos());
    summaryDataPoint.setTimeUnixNano(summaryPointData.getEpochNanos());
    if (summaryPointData.getValues() != null) {
      for (ValueAtQuantile value : summaryPointData.getValues()) {
        summaryDataPoint.addQuantileValues(valueAtQuantileToValueAtQuantile(value));
      }
    }
    summaryDataPoint.setCount(summaryPointData.getCount());
    summaryDataPoint.setSum(summaryPointData.getSum());

    addAttributesToSummaryDataPoint(summaryPointData, summaryDataPoint);

    return summaryDataPoint.build();
  }

  private static void addAttributesToSummaryDataPoint(
      PointData source, SummaryDataPoint.Builder target) {
    target.addAllAttributes(attributesToProto(source.getAttributes()));
  }

  private static HistogramDataPoint histogramPointDataToHistogramDataPoint(
      HistogramPointData histogramPointData) {
    HistogramDataPoint.Builder histogramDataPoint = HistogramDataPoint.newBuilder();

    histogramDataPoint.setStartTimeUnixNano(histogramPointData.getStartEpochNanos());
    histogramDataPoint.setTimeUnixNano(histogramPointData.getEpochNanos());
    if (histogramPointData.getCounts() != null) {
      for (Long count : histogramPointData.getCounts()) {
        histogramDataPoint.addBucketCounts(count);
      }
    }
    if (histogramPointData.getBoundaries() != null) {
      for (Double boundary : histogramPointData.getBoundaries()) {
        histogramDataPoint.addExplicitBounds(boundary);
      }
    }
    if (histogramPointData.getExemplars() != null) {
      for (DoubleExemplarData exemplar : histogramPointData.getExemplars()) {
        histogramDataPoint.addExemplars(doubleExemplarDataToExemplar(exemplar));
      }
    }
    histogramDataPoint.setCount(histogramPointData.getCount());
    histogramDataPoint.setSum(histogramPointData.getSum());
    if (histogramPointData.hasMin()) {
      histogramDataPoint.setMin(histogramPointData.getMin());
    }
    if (histogramPointData.hasMax()) {
      histogramDataPoint.setMax(histogramPointData.getMax());
    }

    addAttributesToHistogramDataPoint(histogramPointData, histogramDataPoint);

    return histogramDataPoint.build();
  }

  private static void addAttributesToHistogramDataPoint(
      HistogramPointData source, HistogramDataPoint.Builder target) {
    target.addAllAttributes(attributesToProto(source.getAttributes()));
  }

  private static ExponentialHistogramDataPoint
      exponentialHistogramPointDataToExponentialHistogramDataPoint(
          ExponentialHistogramPointData exponentialHistogramPointData) {
    ExponentialHistogramDataPoint.Builder exponentialHistogramDataPoint =
        ExponentialHistogramDataPoint.newBuilder();

    exponentialHistogramDataPoint.setStartTimeUnixNano(
        exponentialHistogramPointData.getStartEpochNanos());
    exponentialHistogramDataPoint.setTimeUnixNano(exponentialHistogramPointData.getEpochNanos());
    exponentialHistogramDataPoint.setPositive(
        exponentialHistogramBucketsToBuckets(exponentialHistogramPointData.getPositiveBuckets()));
    exponentialHistogramDataPoint.setNegative(
        exponentialHistogramBucketsToBuckets(exponentialHistogramPointData.getNegativeBuckets()));
    if (exponentialHistogramPointData.getExemplars() != null) {
      for (DoubleExemplarData exemplar : exponentialHistogramPointData.getExemplars()) {
        exponentialHistogramDataPoint.addExemplars(doubleExemplarDataToExemplar(exemplar));
      }
    }
    exponentialHistogramDataPoint.setCount(exponentialHistogramPointData.getCount());
    exponentialHistogramDataPoint.setSum(exponentialHistogramPointData.getSum());
    exponentialHistogramDataPoint.setScale(exponentialHistogramPointData.getScale());
    exponentialHistogramDataPoint.setZeroCount(exponentialHistogramPointData.getZeroCount());
    if (exponentialHistogramPointData.hasMin()) {
      exponentialHistogramDataPoint.setMin(exponentialHistogramPointData.getMin());
    }
    if (exponentialHistogramPointData.hasMax()) {
      exponentialHistogramDataPoint.setMax(exponentialHistogramPointData.getMax());
    }

    addAttributesToExponentialHistogramDataPoint(
        exponentialHistogramPointData, exponentialHistogramDataPoint);

    return exponentialHistogramDataPoint.build();
  }

  private static void addAttributesToExponentialHistogramDataPoint(
      ExponentialHistogramPointData source, ExponentialHistogramDataPoint.Builder target) {
    target.addAllAttributes(attributesToProto(source.getAttributes()));
  }

  private static ExponentialHistogramDataPoint.Buckets exponentialHistogramBucketsToBuckets(
      ExponentialHistogramBuckets source) {
    ExponentialHistogramDataPoint.Buckets.Builder buckets =
        ExponentialHistogramDataPoint.Buckets.newBuilder();

    if (source.getBucketCounts() != null) {
      for (Long bucketCount : source.getBucketCounts()) {
        buckets.addBucketCounts(bucketCount);
      }
    }
    buckets.setOffset(source.getOffset());

    return buckets.build();
  }

  private static Exemplar doubleExemplarDataToExemplar(DoubleExemplarData doubleExemplarData) {
    Exemplar.Builder exemplar = Exemplar.newBuilder();

    exemplar.setTimeUnixNano(doubleExemplarData.getEpochNanos());
    exemplar.setAsDouble(doubleExemplarData.getValue());

    addExtrasToExemplar(doubleExemplarData, exemplar);

    return exemplar.build();
  }

  private static Exemplar longExemplarDataToExemplar(LongExemplarData doubleExemplarData) {
    Exemplar.Builder exemplar = Exemplar.newBuilder();

    exemplar.setTimeUnixNano(doubleExemplarData.getEpochNanos());
    exemplar.setAsInt(doubleExemplarData.getValue());

    addExtrasToExemplar(doubleExemplarData, exemplar);

    return exemplar.build();
  }

  private static void addExtrasToExemplar(ExemplarData source, Exemplar.Builder target) {
    target.addAllFilteredAttributes(attributesToProto(source.getFilteredAttributes()));
    SpanContext spanContext = source.getSpanContext();
    target.setSpanId(ByteStringMapper.getInstance().stringToProto(spanContext.getSpanId()));
    target.setTraceId(ByteStringMapper.getInstance().stringToProto(spanContext.getTraceId()));
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
        aggregationTemporality = AggregationTemporality.UNRECOGNIZED;
    }

    return aggregationTemporality;
  }

  private static SummaryData mapSummaryToSdk(Summary summary) {
    return ImmutableSummaryData.create(
        summaryDataPointListToSummaryPointDataCollection(summary.getDataPointsList()));
  }

  private static HistogramData mapHistogramToSdk(Histogram histogram) {
    return ImmutableHistogramData.create(
        mapAggregationTemporalityToSdk(histogram.getAggregationTemporality()),
        histogramDataPointListToHistogramPointDataCollection(histogram.getDataPointsList()));
  }

  private static ExponentialHistogramData mapExponentialHistogramToSdk(
      ExponentialHistogram source) {
    return ImmutableExponentialHistogramData.create(
        mapAggregationTemporalityToSdk(source.getAggregationTemporality()),
        exponentialHistogramDataPointListToExponentialHistogramPointDataCollection(
            source.getDataPointsList()));
  }

  private static ExponentialHistogramPointData
      exponentialHistogramDataPointToExponentialHistogramPointData(
          ExponentialHistogramDataPoint source) {
    return ImmutableExponentialHistogramPointData.create(
        source.getScale(),
        source.getSum(),
        source.getZeroCount(),
        source.hasMin(),
        source.getMin(),
        source.hasMax(),
        source.getMax(),
        mapBucketsFromProto(source.getPositive(), source.getScale()),
        mapBucketsFromProto(source.getNegative(), source.getScale()),
        source.getStartTimeUnixNano(),
        source.getTimeUnixNano(),
        protoToAttributes(source.getAttributesList()),
        exemplarListToDoubleExemplarDataList(source.getExemplarsList()));
  }

  private static HistogramPointData histogramDataPointToHistogramPointData(
      HistogramDataPoint source) {
    return ImmutableHistogramPointData.create(
        source.getStartTimeUnixNano(),
        source.getTimeUnixNano(),
        protoToAttributes(source.getAttributesList()),
        source.getSum(),
        source.hasMin(),
        source.getMin(),
        source.hasMax(),
        source.getMax(),
        source.getExplicitBoundsList(),
        source.getBucketCountsList(),
        exemplarListToDoubleExemplarDataList(source.getExemplarsList()));
  }

  private static DoubleExemplarData exemplarToDoubleExemplarData(Exemplar source) {
    return ImmutableDoubleExemplarData.create(
        protoToAttributes(source.getFilteredAttributesList()),
        source.getTimeUnixNano(),
        createForExemplar(source),
        source.getAsDouble());
  }

  private static LongExemplarData exemplarToLongExemplarData(Exemplar source) {
    return ImmutableLongExemplarData.create(
        protoToAttributes(source.getFilteredAttributesList()),
        source.getTimeUnixNano(),
        createForExemplar(source),
        source.getAsInt());
  }

  private static SpanContext createForExemplar(Exemplar value) {
    return SpanContext.create(
        ByteStringMapper.getInstance().protoToString(value.getTraceId()),
        ByteStringMapper.getInstance().protoToString(value.getSpanId()),
        TraceFlags.getSampled(),
        TraceState.getDefault());
  }

  private static SummaryPointData summaryDataPointToSummaryPointData(SummaryDataPoint source) {
    return ImmutableSummaryPointData.create(
        source.getStartTimeUnixNano(),
        source.getTimeUnixNano(),
        protoToAttributes(source.getAttributesList()),
        source.getCount(),
        source.getSum(),
        valueAtQuantileListToValueAtQuantileList(source.getQuantileValuesList()));
  }

  private static ValueAtQuantile mapFromSummaryValueAtQuantileProto(
      SummaryDataPoint.ValueAtQuantile source) {
    return ImmutableValueAtQuantile.create(source.getQuantile(), source.getValue());
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
        numberDataPointListToLongPointDataCollection(gauge.getDataPointsList()));
  }

  private static GaugeData<DoublePointData> mapDoubleGaugeToSdk(Gauge gauge) {
    return ImmutableGaugeData.create(
        numberDataPointListToDoublePointDataCollection(gauge.getDataPointsList()));
  }

  private static SumData<LongPointData> mapLongSumToSdk(Sum sum) {
    return ImmutableSumData.create(
        sum.getIsMonotonic(),
        mapAggregationTemporalityToSdk(sum.getAggregationTemporality()),
        numberDataPointListToLongPointDataCollection(sum.getDataPointsList()));
  }

  private static SumData<DoublePointData> mapDoubleSumToSdk(Sum sum) {
    return ImmutableSumData.create(
        sum.getIsMonotonic(),
        mapAggregationTemporalityToSdk(sum.getAggregationTemporality()),
        numberDataPointListToDoublePointDataCollection(sum.getDataPointsList()));
  }

  private static DoublePointData mapDoubleNumberDataPointToSdk(NumberDataPoint source) {
    return ImmutableDoublePointData.create(
        source.getStartTimeUnixNano(),
        source.getTimeUnixNano(),
        protoToAttributes(source.getAttributesList()),
        source.getAsDouble(),
        exemplarListToDoubleExemplarDataList(source.getExemplarsList()));
  }

  private static LongPointData mapLongNumberDataPointToSdk(NumberDataPoint source) {
    return ImmutableLongPointData.create(
        source.getStartTimeUnixNano(),
        source.getTimeUnixNano(),
        protoToAttributes(source.getAttributesList()),
        source.getAsInt(),
        exemplarListToLongExemplarDataList(source.getExemplarsList()));
  }

  private static SummaryDataPoint.ValueAtQuantile valueAtQuantileToValueAtQuantile(
      ValueAtQuantile valueAtQuantile) {
    SummaryDataPoint.ValueAtQuantile.Builder builder =
        SummaryDataPoint.ValueAtQuantile.newBuilder();

    builder.setQuantile(valueAtQuantile.getQuantile());
    builder.setValue(valueAtQuantile.getValue());

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
    return ImmutableExponentialHistogramBuckets.create(
        scale, source.getOffset(), source.getBucketCountsList());
  }

  private static List<KeyValue> attributesToProto(Attributes source) {
    return AttributesMapper.getInstance().attributesToProto(source);
  }

  private static Attributes protoToAttributes(List<KeyValue> source) {
    return AttributesMapper.getInstance().protoToAttributes(source);
  }

  private static final class DataWithType {
    public final Data<?> data;
    public final MetricDataType type;

    private DataWithType(Data<?> data, MetricDataType type) {
      this.data = data;
      this.type = type;
    }
  }
}
