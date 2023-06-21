package io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.proto.metrics;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
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
import io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.metrics.models.datapoints.data.DoubleExemplarDataImpl;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.metrics.models.datapoints.data.ExemplarDataBuilder;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.metrics.models.datapoints.data.ExponentialHistogramBucketsImpl;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.metrics.models.datapoints.data.LongExemplarDataImpl;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.metrics.models.datapoints.data.ValueAtQuantileImpl;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.proto.common.AttributesMapper;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.proto.common.ByteStringMapper;
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
import io.opentelemetry.sdk.resources.Resource;
import java.util.ArrayList;
import java.util.List;
import org.mapstruct.AfterMapping;
import org.mapstruct.BeanMapping;
import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Context;
import org.mapstruct.EnumMapping;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.ValueMapping;

@Mapper(
    collectionMappingStrategy = CollectionMappingStrategy.TARGET_IMMUTABLE,
    unmappedTargetPolicy = ReportingPolicy.IGNORE)
public abstract class MetricDataMapper {

  public static final MetricDataMapper INSTANCE = new MetricDataMapperImpl();

  public abstract Metric mapToProto(MetricData source);

  @BeanMapping(resultType = MetricDataImpl.class)
  public abstract MetricData mapToSdk(
      Metric source, @Context Resource resource, @Context InstrumentationScopeInfo scope);

  @SuppressWarnings("unchecked")
  @AfterMapping
  protected void addDataToProto(MetricData source, @MappingTarget Metric.Builder target) {
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

  @AfterMapping
  protected void addDataToSdk(
      Metric source,
      @MappingTarget MetricDataImpl.Builder target,
      @Context Resource resource,
      @Context InstrumentationScopeInfo scope) {
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

  @Mapping(target = "dataPointsList", source = "points")
  protected abstract Gauge mapLongGaugeToProto(GaugeData<LongPointData> data);

  @Mapping(target = "dataPointsList", source = "points")
  protected abstract Gauge mapDoubleGaugeToProto(GaugeData<DoublePointData> data);

  @Mapping(target = "dataPointsList", source = "points")
  @Mapping(target = "isMonotonic", source = "monotonic")
  protected abstract Sum mapLongSumToProto(SumData<LongPointData> data);

  @Mapping(target = "dataPointsList", source = "points")
  @Mapping(target = "isMonotonic", source = "monotonic")
  protected abstract Sum mapDoubleSumToProto(SumData<DoublePointData> data);

  @Mapping(target = "dataPointsList", source = "points")
  protected abstract Summary mapSummaryToProto(SummaryData data);

  @Mapping(target = "dataPointsList", source = "points")
  protected abstract Histogram mapHistogramToProto(HistogramData data);

  @Mapping(target = "dataPointsList", source = "points")
  protected abstract ExponentialHistogram mapExponentialHistogramToProto(
      ExponentialHistogramData data);

  @Mapping(target = "startTimeUnixNano", source = "startEpochNanos")
  @Mapping(target = "timeUnixNano", source = "epochNanos")
  @Mapping(target = "asInt", source = "value")
  @Mapping(target = "exemplarsList", source = "exemplars")
  protected abstract NumberDataPoint longPointDataToNumberDataPoint(LongPointData source);

  @Mapping(target = "startTimeUnixNano", source = "startEpochNanos")
  @Mapping(target = "timeUnixNano", source = "epochNanos")
  @Mapping(target = "asDouble", source = "value")
  @Mapping(target = "exemplarsList", source = "exemplars")
  protected abstract NumberDataPoint doublePointDataToNumberDataPoint(DoublePointData source);

  @Mapping(target = "startTimeUnixNano", source = "startEpochNanos")
  @Mapping(target = "timeUnixNano", source = "epochNanos")
  @Mapping(target = "quantileValuesList", source = "values")
  protected abstract SummaryDataPoint summaryPointDataToSummaryDataPoint(
      SummaryPointData summaryPointData);

  @Mapping(target = "startTimeUnixNano", source = "startEpochNanos")
  @Mapping(target = "timeUnixNano", source = "epochNanos")
  @Mapping(target = "bucketCountsList", source = "counts")
  @Mapping(target = "explicitBoundsList", source = "boundaries")
  @Mapping(target = "exemplarsList", source = "exemplars")
  protected abstract HistogramDataPoint histogramPointDataToHistogramDataPoint(
      HistogramPointData histogramPointData);

  @Mapping(target = "startTimeUnixNano", source = "startEpochNanos")
  @Mapping(target = "timeUnixNano", source = "epochNanos")
  @Mapping(target = "positive", source = "positiveBuckets")
  @Mapping(target = "negative", source = "negativeBuckets")
  @Mapping(target = "exemplarsList", source = "exemplars")
  protected abstract ExponentialHistogramDataPoint
      exponentialHistogramPointDataToExponentialHistogramDataPoint(
          ExponentialHistogramPointData exponentialHistogramPointData);

  @Mapping(target = "bucketCountsList", source = "bucketCounts")
  @Mapping(target = "offset")
  protected abstract ExponentialHistogramDataPoint.Buckets exponentialHistogramBucketsToBuckets(
      ExponentialHistogramBuckets source);

  @Mapping(target = "timeUnixNano", source = "epochNanos")
  @Mapping(target = "asDouble", source = "value")
  protected abstract Exemplar doubleExemplarDataToExemplar(DoubleExemplarData doubleExemplarData);

  @Mapping(target = "timeUnixNano", source = "epochNanos")
  @Mapping(target = "asInt", source = "value")
  protected abstract Exemplar longExemplarDataToExemplar(LongExemplarData doubleExemplarData);

  @AfterMapping
  protected void addAttributesToNumberDataPoint(
      PointData source, @MappingTarget NumberDataPoint.Builder target) {
    target.addAllAttributes(attributesToProto(source.getAttributes()));
  }

  @AfterMapping
  protected void addAttributesToSummaryDataPoint(
      PointData source, @MappingTarget SummaryDataPoint.Builder target) {
    target.addAllAttributes(attributesToProto(source.getAttributes()));
  }

  @AfterMapping
  protected void addExtrasToExemplar(ExemplarData source, @MappingTarget Exemplar.Builder target) {
    target.addAllFilteredAttributes(attributesToProto(source.getFilteredAttributes()));
    SpanContext spanContext = source.getSpanContext();
    target.setSpanId(ByteStringMapper.INSTANCE.stringToProto(spanContext.getSpanId()));
    target.setTraceId(ByteStringMapper.INSTANCE.stringToProto(spanContext.getTraceId()));
  }

  @AfterMapping
  protected void addAttributesToExponentialHistogramDataPoint(
      ExponentialHistogramPointData source,
      @MappingTarget ExponentialHistogramDataPoint.Builder target) {
    target.addAllAttributes(attributesToProto(source.getAttributes()));
  }

  @AfterMapping
  protected void addAttributesToHistogramDataPoint(
      HistogramPointData source, @MappingTarget HistogramDataPoint.Builder target) {
    target.addAllAttributes(attributesToProto(source.getAttributes()));
  }

  @EnumMapping(
      nameTransformationStrategy = MappingConstants.PREFIX_TRANSFORMATION,
      configuration = "AGGREGATION_TEMPORALITY_")
  protected abstract AggregationTemporality mapAggregationTemporalityToProto(
      io.opentelemetry.sdk.metrics.data.AggregationTemporality source);

  // FROM PROTO
  private DataWithType mapGaugeToSdk(Gauge gauge) {
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

  private DataWithType mapSumToSdk(Sum sum) {
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

  @InheritInverseConfiguration
  @BeanMapping(resultType = SummaryDataImpl.class)
  protected abstract SummaryData mapSummaryToSdk(Summary summary);

  @InheritInverseConfiguration
  @BeanMapping(resultType = HistogramDataImpl.class)
  protected abstract HistogramData mapHistogramToSdk(Histogram histogram);

  @InheritInverseConfiguration
  @BeanMapping(resultType = ExponentialHistogramDataImpl.class)
  protected abstract ExponentialHistogramData mapExponentialHistogramToSdk(
      ExponentialHistogram source);

  @Mapping(target = "exemplars", source = "exemplarsList")
  @BeanMapping(resultType = ExponentialHistogramPointDataImpl.class)
  protected abstract ExponentialHistogramPointData
      exponentialHistogramDataPointToExponentialHistogramPointData(
          ExponentialHistogramDataPoint source);

  @InheritInverseConfiguration
  @BeanMapping(resultType = HistogramPointDataImpl.class)
  protected abstract HistogramPointData histogramDataPointToHistogramPointData(
      HistogramDataPoint source);

  @InheritInverseConfiguration
  @BeanMapping(resultType = DoubleExemplarDataImpl.class)
  protected abstract DoubleExemplarData exemplarToDoubleExemplarData(Exemplar source);

  @InheritInverseConfiguration
  @BeanMapping(resultType = LongExemplarDataImpl.class)
  protected abstract LongExemplarData exemplarToLongExemplarData(Exemplar source);

  @InheritInverseConfiguration
  @BeanMapping(resultType = SummaryPointDataImpl.class)
  protected abstract SummaryPointData summaryDataPointToSummaryPointData(SummaryDataPoint source);

  @BeanMapping(resultType = ValueAtQuantileImpl.class)
  protected abstract ValueAtQuantile mapFromSummaryValueAtQuantileProto(
      SummaryDataPoint.ValueAtQuantile source);

  @EnumMapping(
      nameTransformationStrategy = MappingConstants.STRIP_PREFIX_TRANSFORMATION,
      configuration = "AGGREGATION_TEMPORALITY_")
  @ValueMapping(source = MappingConstants.ANY_REMAINING, target = MappingConstants.THROW_EXCEPTION)
  protected abstract io.opentelemetry.sdk.metrics.data.AggregationTemporality
      mapAggregationTemporalityToSdk(AggregationTemporality source);

  @InheritInverseConfiguration
  @BeanMapping(resultType = GaugeDataImpl.LongData.class)
  protected abstract GaugeData<LongPointData> mapLongGaugeToSdk(Gauge gauge);

  @InheritInverseConfiguration
  @BeanMapping(resultType = GaugeDataImpl.DoubleData.class)
  protected abstract GaugeData<DoublePointData> mapDoubleGaugeToSdk(Gauge gauge);

  @InheritInverseConfiguration
  @BeanMapping(resultType = SumDataImpl.LongData.class)
  protected abstract SumData<LongPointData> mapLongSumToSdk(Sum sum);

  @InheritInverseConfiguration
  @BeanMapping(resultType = SumDataImpl.DoubleData.class)
  protected abstract SumData<DoublePointData> mapDoubleSumToSdk(Sum sum);

  @InheritInverseConfiguration
  @BeanMapping(resultType = DoublePointDataImpl.class)
  protected abstract DoublePointData mapDoubleNumberDataPointToSdk(NumberDataPoint source);

  @InheritInverseConfiguration
  @BeanMapping(resultType = LongPointDataImpl.class)
  protected abstract LongPointData mapLongNumberDataPointToSdk(NumberDataPoint source);

  @AfterMapping
  protected void addAttributesFromNumberDataPoint(
      NumberDataPoint source, @MappingTarget PointDataBuilder<?> target) {
    target.setAttributes(protoToAttributes(source.getAttributesList()));
  }

  @AfterMapping
  protected void addAttributesFromSummaryDataPoint(
      SummaryDataPoint source, @MappingTarget SummaryPointDataImpl.Builder target) {
    target.setAttributes(protoToAttributes(source.getAttributesList()));
  }

  @AfterMapping
  protected void addAttributesFromHistogramDataPoint(
      HistogramDataPoint source, @MappingTarget HistogramPointDataImpl.Builder target) {
    target.setAttributes(protoToAttributes(source.getAttributesList()));
  }

  @AfterMapping
  protected void addExtrasFromExemplar(
      Exemplar source, @MappingTarget ExemplarDataBuilder<?> target) {
    target.setFilteredAttributes(protoToAttributes(source.getFilteredAttributesList()));
    target.setSpanContext(
        SpanContext.create(
            ByteStringMapper.INSTANCE.protoToString(source.getTraceId()),
            ByteStringMapper.INSTANCE.protoToString(source.getSpanId()),
            TraceFlags.getSampled(),
            TraceState.getDefault()));
  }

  @AfterMapping
  protected void addBucketsExtrasFromProto(
      ExponentialHistogramDataPoint source,
      @MappingTarget ExponentialHistogramPointDataImpl.Builder target) {
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

  protected ExponentialHistogramBuckets mapBucketsFromProto(
      ExponentialHistogramDataPoint.Buckets source, Integer scale) {
    List<Long> bucketCounts = new ArrayList<>();
    long totalCount = 0;
    for (Long bucketCount : source.getBucketCountsList()) {
      bucketCounts.add(bucketCount);
      totalCount += bucketCount;
    }
    return ExponentialHistogramBucketsImpl.builder()
        .setOffset(source.getOffset())
        .setBucketCounts(bucketCounts)
        .setTotalCount(totalCount)
        .setScale(scale)
        .build();
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
