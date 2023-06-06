package io.opentelemetry.contrib.disk.buffer.internal.mapping.metrics.models.datapoints;

import com.google.auto.value.AutoValue;
import io.opentelemetry.sdk.metrics.data.ExemplarData;
import io.opentelemetry.sdk.metrics.data.SummaryPointData;
import io.opentelemetry.sdk.metrics.data.ValueAtQuantile;
import java.util.Collections;
import java.util.List;

@AutoValue
public abstract class SummaryPointDataImpl implements SummaryPointData {

  public static Builder builder() {
    return new AutoValue_SummaryPointDataImpl.Builder();
  }

  @Override
  public List<? extends ExemplarData> getExemplars() {
    return Collections.emptyList();
  }

  @AutoValue.Builder
  public abstract static class Builder implements PointDataBuilder<Builder> {
    public abstract Builder setCount(Long value);

    public abstract Builder setSum(Double value);

    public abstract Builder setValues(List<ValueAtQuantile> value);

    public abstract SummaryPointDataImpl build();
  }
}
