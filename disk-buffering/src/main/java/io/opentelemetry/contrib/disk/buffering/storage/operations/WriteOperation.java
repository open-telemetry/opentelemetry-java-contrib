package io.opentelemetry.contrib.disk.buffering.storage.operations;

import javax.annotation.Nullable;

public interface WriteOperation {
  boolean isSuccessful();

  @Nullable
  Throwable getError();
}
