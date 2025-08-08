package io.opentelemetry.contrib.disk.buffering.storage.result;

import javax.annotation.Nullable;

public interface WriteResult {
  boolean isSuccessful();

  @Nullable
  Throwable getError();
}
