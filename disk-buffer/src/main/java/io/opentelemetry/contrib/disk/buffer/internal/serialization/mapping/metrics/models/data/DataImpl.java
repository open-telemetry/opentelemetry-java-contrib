package io.opentelemetry.contrib.disk.buffer.internal.serialization.mapping.metrics.models.data;

import io.opentelemetry.sdk.metrics.data.Data;
import io.opentelemetry.sdk.metrics.data.PointData;
import java.util.ArrayList;
import java.util.Collection;

public abstract class DataImpl<T extends PointData> implements Data<T> {
  public Collection<T> points = new ArrayList<>();

  @Override
  public Collection<T> getPoints() {
    return points;
  }
}
