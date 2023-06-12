/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.metrics.models.datapoints;

import com.google.auto.value.AutoValue;
import io.opentelemetry.sdk.metrics.data.DoubleExemplarData;
import io.opentelemetry.sdk.metrics.data.HistogramPointData;
import java.util.List;

@AutoValue
public abstract class HistogramPointDataImpl implements HistogramPointData {

  public static Builder builder() {
    return new AutoValue_HistogramPointDataImpl.Builder();
  }

  @Override
  public boolean hasMin() {
    return getMin() > -1;
  }

  @Override
  public boolean hasMax() {
    return getMax() > -1;
  }

  @AutoValue.Builder
  public abstract static class Builder implements PointDataBuilder<Builder> {
    public abstract Builder setSum(Double value);

    public abstract Builder setCount(Long value);

    public abstract Builder setMin(Double value);

    public abstract Builder setMax(Double value);

    public abstract Builder setBoundaries(List<Double> value);

    public abstract Builder setCounts(List<Long> value);

    public abstract Builder setExemplars(List<DoubleExemplarData> value);

    public abstract HistogramPointDataImpl build();
  }
}
