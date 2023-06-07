package io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.metrics.models.data.base;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.opentelemetry.sdk.metrics.data.PointData;
import java.util.Collection;

public interface DataBuilder<P extends PointData, T extends DataBuilder<P, ?>> {

  @CanIgnoreReturnValue
  T setPoints(Collection<P> value);
}
