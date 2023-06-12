/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.metrics;

import io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.metrics.models.data.ExponentialHistogramDataImpl;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.metrics.models.data.GaugeDataImpl;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.metrics.models.data.HistogramDataImpl;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.metrics.models.data.SumDataImpl;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.metrics.models.data.SummaryDataImpl;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.models.metrics.data.DataJson;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.models.metrics.data.ExponentialHistogram;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.models.metrics.data.Gauge;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.models.metrics.data.Histogram;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.models.metrics.data.Sum;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.models.metrics.data.Summary;
import io.opentelemetry.sdk.metrics.data.AggregationTemporality;
import io.opentelemetry.sdk.metrics.data.Data;
import io.opentelemetry.sdk.metrics.data.DoublePointData;
import io.opentelemetry.sdk.metrics.data.ExponentialHistogramData;
import io.opentelemetry.sdk.metrics.data.GaugeData;
import io.opentelemetry.sdk.metrics.data.HistogramData;
import io.opentelemetry.sdk.metrics.data.LongPointData;
import io.opentelemetry.sdk.metrics.data.MetricDataType;
import io.opentelemetry.sdk.metrics.data.SumData;
import io.opentelemetry.sdk.metrics.data.SummaryData;
import org.mapstruct.BeanMapping;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.SubclassExhaustiveStrategy;

@Mapper(
    uses = DataPointMapper.class,
    subclassExhaustiveStrategy = SubclassExhaustiveStrategy.RUNTIME_EXCEPTION)
public abstract class MetricDataMapper {

  public static final MetricDataMapper INSTANCE = new MetricDataMapperImpl();

  @SuppressWarnings("unchecked")
  public DataJson<?> metricDataToJson(Data<?> data, @Context MetricDataType type) {
    switch (type) {
      case LONG_GAUGE:
        return longGaugeToJson((GaugeData<LongPointData>) data);
      case DOUBLE_GAUGE:
        return doubleGaugeToJson((GaugeData<DoublePointData>) data);
      case LONG_SUM:
        return longSumToJson((SumData<LongPointData>) data);
      case DOUBLE_SUM:
        return doubleSumToJson((SumData<DoublePointData>) data);
      case SUMMARY:
        return summaryToJson((SummaryData) data);
      case HISTOGRAM:
        return histogramToJson((HistogramData) data);
      case EXPONENTIAL_HISTOGRAM:
        return exponentialHistogramToJson((ExponentialHistogramData) data);
    }
    throw new UnsupportedOperationException();
  }

  @BeanMapping(resultType = Summary.class)
  protected abstract DataJson<?> summaryToJson(SummaryData source);

  @BeanMapping(resultType = Histogram.class)
  protected abstract DataJson<?> histogramToJson(HistogramData source);

  @BeanMapping(resultType = Sum.class)
  protected abstract DataJson<?> longSumToJson(SumData<LongPointData> source);

  @BeanMapping(resultType = Gauge.class)
  protected abstract DataJson<?> longGaugeToJson(GaugeData<LongPointData> source);

  @BeanMapping(resultType = Gauge.class)
  protected abstract DataJson<?> doubleGaugeToJson(GaugeData<DoublePointData> source);

  @BeanMapping(resultType = Sum.class)
  protected abstract DataJson<?> doubleSumToJson(SumData<DoublePointData> source);

  @BeanMapping(resultType = ExponentialHistogram.class)
  protected abstract DataJson<?> exponentialHistogramToJson(ExponentialHistogramData source);

  protected Integer aggregationTemporalityToNumber(AggregationTemporality aggregationTemporality) {
    return aggregationTemporality.ordinal() + 1;
  }

  // FROM JSON
  public Data<?> jsonToMetricData(DataJson<?> source, @Context MetricDataType type) {
    switch (type) {
      case LONG_GAUGE:
        return jsonToLongGauge((Gauge) source);
      case DOUBLE_GAUGE:
        return jsonToDoubleGauge((Gauge) source);
      case LONG_SUM:
        return jsonToLongSum((Sum) source);
      case DOUBLE_SUM:
        return jsonToDoubleSum((Sum) source);
      case SUMMARY:
        return jsonToSummary((Summary) source);
      case HISTOGRAM:
        return jsonToHistogram((Histogram) source);
      case EXPONENTIAL_HISTOGRAM:
        return jsonToExponentialHistogram((ExponentialHistogram) source);
    }
    throw new UnsupportedOperationException();
  }

  @BeanMapping(resultType = GaugeDataImpl.LongData.class)
  protected abstract GaugeData<LongPointData> jsonToLongGauge(Gauge source);

  @BeanMapping(resultType = GaugeDataImpl.DoubleData.class)
  protected abstract GaugeData<DoublePointData> jsonToDoubleGauge(Gauge source);

  @BeanMapping(resultType = SumDataImpl.LongData.class)
  protected abstract SumData<LongPointData> jsonToLongSum(Sum source);

  @BeanMapping(resultType = SumDataImpl.DoubleData.class)
  protected abstract SumData<DoublePointData> jsonToDoubleSum(Sum source);

  @BeanMapping(resultType = SummaryDataImpl.class)
  protected abstract SummaryData jsonToSummary(Summary source);

  @BeanMapping(resultType = HistogramDataImpl.class)
  protected abstract HistogramData jsonToHistogram(Histogram source);

  @BeanMapping(resultType = ExponentialHistogramDataImpl.class)
  protected abstract ExponentialHistogramData jsonToExponentialHistogram(
      ExponentialHistogram source);

  protected AggregationTemporality jsonToAggregationTemporality(Integer source) {
    int ordinal = source - 1;
    for (AggregationTemporality value : AggregationTemporality.values()) {
      if (value.ordinal() == ordinal) {
        return value;
      }
    }
    throw new IllegalArgumentException();
  }
}
