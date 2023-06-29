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
import io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.metrics.models.MetricDataImpl;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.metrics.models.data.ExponentialHistogramDataImpl;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.metrics.models.data.GaugeDataImpl;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.metrics.models.data.HistogramDataImpl;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.metrics.models.data.SumDataImpl;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.metrics.models.data.SummaryDataImpl;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.metrics.models.datapoints.DoublePointDataImpl;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.metrics.models.datapoints.ExponentialHistogramPointDataImpl;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.metrics.models.datapoints.HistogramPointDataImpl;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.metrics.models.datapoints.LongPointDataImpl;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.metrics.models.datapoints.PointDataBuilder;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.metrics.models.datapoints.SummaryPointDataImpl;
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
import io.opentelemetry.sdk.metrics.internal.data.ImmutableExponentialHistogramBuckets;
import io.opentelemetry.sdk.metrics.internal.data.ImmutableLongExemplarData;
import io.opentelemetry.sdk.metrics.internal.data.ImmutableValueAtQuantile;
import io.opentelemetry.sdk.resources.Resource;
import java.util.ArrayList;
import java.util.List;

public final class MetricDataMapper {

  public static final MetricDataMapper INSTANCE = new MetricDataMapper();

  public Metric mapToProto(MetricData source) {
    Metric.Builder metric = Metric.newBuilder();

    metric.setName(source.getName());
    metric.setDescription(source.getDescription());
    metric.setUnit(source.getUnit());

    addDataToProto(source, metric);

    return metric.build();
  }

  public MetricData mapToSdk(Metric source, Resource resource, InstrumentationScopeInfo scope) {
    MetricDataImpl.Builder metricData = MetricDataImpl.builder();

    metricData.setName(source.getName());
    metricData.setDescription(source.getDescription());
    metricData.setUnit(source.getUnit());

    addDataToSdk(source, metricData, resource, scope);

    return metricData.build();
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

  private static void addDataToSdk(
      Metric source,
      MetricDataImpl.Builder target,
      Resource resource,
      InstrumentationScopeInfo scope) {
    target.setResource(resource);
    target.setInstrumentationScopeInfo(scope);
    switch (source.getDataCase()) {
      case GAUGE:
        DataWithType gaugeDataWithType = mapGaugeToSdk(source.getGauge());
        target.setData(gaugeDataWithType.data);
        target.setType(gaugeDataWithType.type);
        break;
      case SUM:
        DataWithType sumDataWithType = mapSumToSdk(source.getSum());
        target.setData(sumDataWithType.data);
        target.setType(sumDataWithType.type);
        break;
      case SUMMARY:
        target.setData(mapSummaryToSdk(source.getSummary()));
        target.setType(MetricDataType.SUMMARY);
        break;
      case HISTOGRAM:
        target.setData(mapHistogramToSdk(source.getHistogram()));
        target.setType(MetricDataType.HISTOGRAM);
        break;
      case EXPONENTIAL_HISTOGRAM:
        target.setData(mapExponentialHistogramToSdk(source.getExponentialHistogram()));
        target.setType(MetricDataType.EXPONENTIAL_HISTOGRAM);
        break;
      default:
        throw new UnsupportedOperationException();
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
    target.setSpanId(ByteStringMapper.INSTANCE.stringToProto(spanContext.getSpanId()));
    target.setTraceId(ByteStringMapper.INSTANCE.stringToProto(spanContext.getTraceId()));
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
    SummaryDataImpl.Builder summaryData = SummaryDataImpl.builder();

    summaryData.setPoints(
        summaryDataPointListToSummaryPointDataCollection(summary.getDataPointsList()));

    return summaryData.build();
  }

  private static HistogramData mapHistogramToSdk(Histogram histogram) {
    HistogramDataImpl.Builder histogramData = HistogramDataImpl.builder();

    histogramData.setPoints(
        histogramDataPointListToHistogramPointDataCollection(histogram.getDataPointsList()));
    histogramData.setAggregationTemporality(
        mapAggregationTemporalityToSdk(histogram.getAggregationTemporality()));

    return histogramData.build();
  }

  private static ExponentialHistogramData mapExponentialHistogramToSdk(
      ExponentialHistogram source) {
    ExponentialHistogramDataImpl.Builder exponentialHistogramData =
        ExponentialHistogramDataImpl.builder();

    exponentialHistogramData.setPoints(
        exponentialHistogramDataPointListToExponentialHistogramPointDataCollection(
            source.getDataPointsList()));
    exponentialHistogramData.setAggregationTemporality(
        mapAggregationTemporalityToSdk(source.getAggregationTemporality()));

    return exponentialHistogramData.build();
  }

  private static ExponentialHistogramPointData
      exponentialHistogramDataPointToExponentialHistogramPointData(
          ExponentialHistogramDataPoint source) {
    ExponentialHistogramPointDataImpl.Builder exponentialHistogramPointData =
        ExponentialHistogramPointDataImpl.builder();

    exponentialHistogramPointData.setExemplars(
        exemplarListToDoubleExemplarDataList(source.getExemplarsList()));
    exponentialHistogramPointData.setScale(source.getScale());
    if (source.hasSum()) {
      exponentialHistogramPointData.setSum(source.getSum());
    }
    exponentialHistogramPointData.setCount(source.getCount());
    exponentialHistogramPointData.setZeroCount(source.getZeroCount());
    if (source.hasMin()) {
      exponentialHistogramPointData.setMin(source.getMin());
    }
    if (source.hasMax()) {
      exponentialHistogramPointData.setMax(source.getMax());
    }

    addBucketsExtrasFromProto(source, exponentialHistogramPointData);

    return exponentialHistogramPointData.build();
  }

  private static void addBucketsExtrasFromProto(
      ExponentialHistogramDataPoint source, ExponentialHistogramPointDataImpl.Builder target) {
    target.setAttributes(protoToAttributes(source.getAttributesList()));
    target.setStartEpochNanos(source.getStartTimeUnixNano());
    target.setEpochNanos(source.getTimeUnixNano());
    if (source.hasPositive()) {
      target.setPositiveBuckets(mapBucketsFromProto(source.getPositive(), source.getScale()));
    }
    if (source.hasNegative()) {
      target.setNegativeBuckets(mapBucketsFromProto(source.getNegative(), source.getScale()));
    }
  }

  private static HistogramPointData histogramDataPointToHistogramPointData(
      HistogramDataPoint source) {
    HistogramPointDataImpl.Builder histogramPointData = HistogramPointDataImpl.builder();

    histogramPointData.setStartEpochNanos(source.getStartTimeUnixNano());
    histogramPointData.setEpochNanos(source.getTimeUnixNano());
    List<Long> bucketCounts = source.getBucketCountsList();
    histogramPointData.setCounts(new ArrayList<>(bucketCounts));
    List<Double> explicitBounds = source.getExplicitBoundsList();
    histogramPointData.setBoundaries(new ArrayList<>(explicitBounds));
    histogramPointData.setExemplars(
        exemplarListToDoubleExemplarDataList(source.getExemplarsList()));
    if (source.hasSum()) {
      histogramPointData.setSum(source.getSum());
    }
    histogramPointData.setCount(source.getCount());
    if (source.hasMin()) {
      histogramPointData.setMin(source.getMin());
    }
    if (source.hasMax()) {
      histogramPointData.setMax(source.getMax());
    }

    addAttributesFromHistogramDataPoint(source, histogramPointData);

    return histogramPointData.build();
  }

  private static void addAttributesFromHistogramDataPoint(
      HistogramDataPoint source, HistogramPointDataImpl.Builder target) {
    target.setAttributes(protoToAttributes(source.getAttributesList()));
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
        ByteStringMapper.INSTANCE.protoToString(value.getTraceId()),
        ByteStringMapper.INSTANCE.protoToString(value.getSpanId()),
        TraceFlags.getSampled(),
        TraceState.getDefault());
  }

  private static SummaryPointData summaryDataPointToSummaryPointData(SummaryDataPoint source) {
    SummaryPointDataImpl.Builder summaryPointData = SummaryPointDataImpl.builder();

    summaryPointData.setStartEpochNanos(source.getStartTimeUnixNano());
    summaryPointData.setEpochNanos(source.getTimeUnixNano());
    summaryPointData.setValues(
        valueAtQuantileListToValueAtQuantileList(source.getQuantileValuesList()));
    summaryPointData.setCount(source.getCount());
    summaryPointData.setSum(source.getSum());

    addAttributesFromSummaryDataPoint(source, summaryPointData);

    return summaryPointData.build();
  }

  private static void addAttributesFromSummaryDataPoint(
      SummaryDataPoint source, SummaryPointDataImpl.Builder target) {
    target.setAttributes(protoToAttributes(source.getAttributesList()));
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
    GaugeDataImpl.LongData.Builder gaugeData = GaugeDataImpl.LongData.builder();

    gaugeData.setPoints(numberDataPointListToLongPointDataCollection(gauge.getDataPointsList()));

    return gaugeData.build();
  }

  private static GaugeData<DoublePointData> mapDoubleGaugeToSdk(Gauge gauge) {
    GaugeDataImpl.DoubleData.Builder gaugeData = GaugeDataImpl.DoubleData.builder();

    gaugeData.setPoints(numberDataPointListToDoublePointDataCollection(gauge.getDataPointsList()));

    return gaugeData.build();
  }

  private static SumData<LongPointData> mapLongSumToSdk(Sum sum) {
    SumDataImpl.LongData.Builder sumData = SumDataImpl.LongData.builder();

    sumData.setPoints(numberDataPointListToLongPointDataCollection(sum.getDataPointsList()));
    sumData.setMonotonic(sum.getIsMonotonic());
    sumData.setAggregationTemporality(
        mapAggregationTemporalityToSdk(sum.getAggregationTemporality()));

    return sumData.build();
  }

  private static SumData<DoublePointData> mapDoubleSumToSdk(Sum sum) {
    SumDataImpl.DoubleData.Builder sumData = SumDataImpl.DoubleData.builder();

    sumData.setPoints(numberDataPointListToDoublePointDataCollection(sum.getDataPointsList()));
    sumData.setMonotonic(sum.getIsMonotonic());
    sumData.setAggregationTemporality(
        mapAggregationTemporalityToSdk(sum.getAggregationTemporality()));

    return sumData.build();
  }

  private static DoublePointData mapDoubleNumberDataPointToSdk(NumberDataPoint source) {
    DoublePointDataImpl.Builder doublePointData = DoublePointDataImpl.builder();

    doublePointData.setStartEpochNanos(source.getStartTimeUnixNano());
    doublePointData.setEpochNanos(source.getTimeUnixNano());
    if (source.hasAsDouble()) {
      doublePointData.setValue(source.getAsDouble());
    }
    doublePointData.setExemplars(exemplarListToDoubleExemplarDataList(source.getExemplarsList()));

    addAttributesFromNumberDataPoint(source, doublePointData);

    return doublePointData.build();
  }

  private static LongPointData mapLongNumberDataPointToSdk(NumberDataPoint source) {
    LongPointDataImpl.Builder longPointData = LongPointDataImpl.builder();

    longPointData.setStartEpochNanos(source.getStartTimeUnixNano());
    longPointData.setEpochNanos(source.getTimeUnixNano());
    if (source.hasAsInt()) {
      longPointData.setValue(source.getAsInt());
    }
    longPointData.setExemplars(exemplarListToLongExemplarDataList(source.getExemplarsList()));

    addAttributesFromNumberDataPoint(source, longPointData);

    return longPointData.build();
  }

  private static void addAttributesFromNumberDataPoint(
      NumberDataPoint source, PointDataBuilder<?> target) {
    target.setAttributes(protoToAttributes(source.getAttributesList()));
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
    return AttributesMapper.INSTANCE.attributesToProto(source);
  }

  private static Attributes protoToAttributes(List<KeyValue> source) {
    return AttributesMapper.INSTANCE.protoToAttributes(source);
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
