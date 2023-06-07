package io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.metrics.models.data;

import com.google.auto.value.AutoValue;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.metrics.models.data.base.DataBuilder;
import io.opentelemetry.sdk.metrics.data.SummaryData;
import io.opentelemetry.sdk.metrics.data.SummaryPointData;

@AutoValue
public abstract class SummaryDataImpl implements SummaryData {

  public static Builder builder() {
    return new AutoValue_SummaryDataImpl.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder implements DataBuilder<SummaryPointData, Builder> {
    public abstract SummaryDataImpl build();
  }
}
