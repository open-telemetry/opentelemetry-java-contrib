/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.metrics;

import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.metrics.models.datapoints.DoublePointDataImpl;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.metrics.models.datapoints.ExponentialHistogramPointDataImpl;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.metrics.models.datapoints.HistogramPointDataImpl;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.metrics.models.datapoints.LongPointDataImpl;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.metrics.models.datapoints.SummaryPointDataImpl;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.metrics.models.datapoints.data.DoubleExemplarDataImpl;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.metrics.models.datapoints.data.ExemplarDataBuilder;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.metrics.models.datapoints.data.ExponentialHistogramBucketsImpl;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.metrics.models.datapoints.data.LongExemplarDataImpl;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.metrics.models.datapoints.data.ValueAtQuantileImpl;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.models.metrics.datapoints.DataPoint;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.models.metrics.datapoints.ExponentialHistogramDataPoint;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.models.metrics.datapoints.HistogramDataPoint;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.models.metrics.datapoints.NumberDataPoint;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.models.metrics.datapoints.SummaryDataPoint;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.models.metrics.datapoints.data.Buckets;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.models.metrics.datapoints.data.Exemplar;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.models.metrics.datapoints.data.QuantileValue;
import io.opentelemetry.sdk.metrics.data.DoubleExemplarData;
import io.opentelemetry.sdk.metrics.data.DoublePointData;
import io.opentelemetry.sdk.metrics.data.ExemplarData;
import io.opentelemetry.sdk.metrics.data.ExponentialHistogramBuckets;
import io.opentelemetry.sdk.metrics.data.ExponentialHistogramPointData;
import io.opentelemetry.sdk.metrics.data.HistogramPointData;
import io.opentelemetry.sdk.metrics.data.LongExemplarData;
import io.opentelemetry.sdk.metrics.data.LongPointData;
import io.opentelemetry.sdk.metrics.data.PointData;
import io.opentelemetry.sdk.metrics.data.SummaryPointData;
import io.opentelemetry.sdk.metrics.data.ValueAtQuantile;
import org.mapstruct.AfterMapping;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.SubclassExhaustiveStrategy;
import org.mapstruct.SubclassMapping;
import org.mapstruct.factory.Mappers;

@Mapper(
    subclassExhaustiveStrategy = SubclassExhaustiveStrategy.RUNTIME_EXCEPTION,
    unmappedTargetPolicy = ReportingPolicy.IGNORE)
public abstract class DataPointMapper {

  public static final DataPointMapper INSTANCE = Mappers.getMapper(DataPointMapper.class);

  @SubclassMapping(source = LongPointData.class, target = NumberDataPoint.class)
  @SubclassMapping(source = DoublePointData.class, target = NumberDataPoint.class)
  @SubclassMapping(source = HistogramPointData.class, target = HistogramDataPoint.class)
  @SubclassMapping(source = SummaryPointData.class, target = SummaryDataPoint.class)
  @SubclassMapping(
      source = ExponentialHistogramPointData.class,
      target = ExponentialHistogramDataPoint.class)
  public abstract DataPoint pointDataToJson(PointData source);

  protected abstract Buckets exponentialBucketsToJson(ExponentialHistogramBuckets source);

  protected abstract QuantileValue quantileToJson(ValueAtQuantile source);

  @AfterMapping
  protected void spanContextToJson(ExemplarData source, @MappingTarget Exemplar target) {
    target.spanId = source.getSpanContext().getSpanId();
    target.traceId = source.getSpanContext().getTraceId();
  }

  // FROM JSON
  @BeanMapping(resultType = LongPointDataImpl.class)
  @Mapping(target = "value", source = "longValue")
  public abstract LongPointData jsonToLongPointData(NumberDataPoint source);

  @BeanMapping(resultType = DoublePointDataImpl.class)
  @Mapping(target = "value", source = "doubleValue")
  public abstract DoublePointData jsonToDoublePointData(NumberDataPoint source);

  @BeanMapping(resultType = HistogramPointDataImpl.class)
  public abstract HistogramPointData jsonHistogramToPointData(HistogramDataPoint source);

  @BeanMapping(resultType = SummaryPointDataImpl.class)
  public abstract SummaryPointData jsonSummaryToPointData(SummaryDataPoint source);

  @BeanMapping(resultType = ExponentialHistogramPointDataImpl.class)
  public abstract ExponentialHistogramPointData jsonExponentialHistogramToPointData(
      ExponentialHistogramDataPoint source);

  @BeanMapping(resultType = LongExemplarDataImpl.class)
  @Mapping(target = "value", source = "longValue")
  protected abstract LongExemplarData jsonToLongExemplar(Exemplar source);

  @BeanMapping(resultType = DoubleExemplarDataImpl.class)
  @Mapping(target = "value", source = "doubleValue")
  protected abstract DoubleExemplarData jsonToDoubleExemplar(Exemplar source);

  @BeanMapping(resultType = ExponentialHistogramBucketsImpl.class)
  protected abstract ExponentialHistogramBuckets jsonToExponentialBuckets(Buckets source);

  @BeanMapping(resultType = ValueAtQuantileImpl.class)
  protected abstract ValueAtQuantile jsonToQuantile(QuantileValue source);

  @AfterMapping
  protected void jsonToSpanContext(Exemplar source, @MappingTarget ExemplarDataBuilder<?> target) {
    if (source.traceId != null && source.spanId != null) {
      target.setSpanContext(
          SpanContext.create(
              source.traceId, source.spanId, TraceFlags.getSampled(), TraceState.getDefault()));
    }
  }
}
