package io.opentelemetry.contrib.disk.buffer.internal.mapping.metrics.models.datapoints.data;

import com.google.auto.value.AutoValue;
import io.opentelemetry.sdk.metrics.data.ExponentialHistogramBuckets;
import java.util.List;

@AutoValue
public abstract class ExponentialHistogramBucketsImpl implements ExponentialHistogramBuckets {

  public static Builder builder() {
    return new AutoValue_ExponentialHistogramBucketsImpl.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setScale(Integer value);

    public abstract Builder setOffset(Integer value);

    public abstract Builder setBucketCounts(List<Long> value);

    public abstract Builder setTotalCount(Long value);

    public abstract ExponentialHistogramBucketsImpl build();
  }
}
