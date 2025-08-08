/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.storage.result;

import io.opentelemetry.contrib.disk.buffering.storage.SignalStorage;
import javax.annotation.Nullable;

/** The result of a {@link SignalStorage} write operation. */
public interface WriteResult {
  /**
   * Whether the operation succeeded or not.
   *
   * @return `true` if the items have been successfully stored, `false` otherwise.
   */
  boolean isSuccessful();

  /**
   * Provides details of why the operation failed.
   *
   * @return The error (if any) for the failed operation. It must be null for successful operations.
   */
  @Nullable
  Throwable getError();

  static WriteResult create(boolean successful, @Nullable Throwable error) {
    return new DefaultWriteResult(successful, error);
  }
}
