/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

// Includes work from:

/*
 * Prometheus instrumentation library for JVM applications
 * Copyright 2012-2015 The Prometheus Authors
 *
 * This product includes software developed at
 * Boxever Ltd. (http://www.boxever.com/).
 *
 * This product includes software developed at
 * SoundCloud Ltd. (http://soundcloud.com/).
 *
 * This product includes software developed as part of the
 * Ocelli project by Netflix Inc. (https://github.com/Netflix/ocelli/).
 */

package io.opentelemetry.contrib.metrics.prometheus.clientbridge;

import io.opentelemetry.sdk.metrics.data.HistogramPointData;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.data.PointData;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/** Serializes metrics into Prometheus exposition formats. */
// Adapted from
// https://github.com/prometheus/client_java/blob/master/simpleclient_common/src/main/java/io/prometheus/client/exporter/common/TextFormat.java
class Serializer {

  /**
   * Returns the lower bound of a bucket (all values would have been greater than).
   *
   * @param bucketIndex The bucket index, should match {@link HistogramPointData#getCounts()} index.
   */
  static double getBucketLowerBound(HistogramPointData point, int bucketIndex) {
    return bucketIndex > 0 ? point.getBoundaries().get(bucketIndex - 1) : Double.NEGATIVE_INFINITY;
  }

  /**
   * Returns the upper inclusive bound of a bucket (all values would have been less then or equal).
   *
   * @param bucketIndex The bucket index, should match {@link HistogramPointData#getCounts()} index.
   */
  static double getBucketUpperBound(HistogramPointData point, int bucketIndex) {
    List<Double> boundaries = point.getBoundaries();
    return (bucketIndex < boundaries.size())
        ? boundaries.get(bucketIndex)
        : Double.POSITIVE_INFINITY;
  }

  static Collection<? extends PointData> getPoints(MetricData metricData) {
    switch (metricData.getType()) {
      case DOUBLE_GAUGE:
        return metricData.getDoubleGaugeData().getPoints();
      case DOUBLE_SUM:
        return metricData.getDoubleSumData().getPoints();
      case LONG_GAUGE:
        return metricData.getLongGaugeData().getPoints();
      case LONG_SUM:
        return metricData.getLongSumData().getPoints();
      case SUMMARY:
        return metricData.getSummaryData().getPoints();
      case HISTOGRAM:
        return metricData.getHistogramData().getPoints();
      case EXPONENTIAL_HISTOGRAM:
        return metricData.getExponentialHistogramData().getPoints();
    }
    return Collections.emptyList();
  }

  private Serializer() {}
}
