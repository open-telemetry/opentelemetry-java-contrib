package io.opentelemetry.contrib.disk.buffering.storage.result;

import javax.annotation.Nullable;

final class DefaultWriteResult implements WriteResult {
  private final boolean successful;
  @Nullable private final Throwable error;

  DefaultWriteResult(boolean successful, @Nullable Throwable error) {
    this.successful = successful;
    this.error = error;
  }

  @Override
  public boolean isSuccessful() {
    return successful;
  }

  @Nullable
  @Override
  public Throwable getError() {
    return error;
  }
}
