package io.opentelemetry.contrib.disk.buffering.storage.result;

import javax.annotation.Nullable;

public interface WriteResult {
  boolean isSuccessful();

  @Nullable
  Throwable getError();

  static WriteResult create(boolean successful, @Nullable Throwable error) {
    return new DefaultWriteResult(successful, error);
  }
}
