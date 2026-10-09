/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.storage.impl;

import java.io.File;

/** Notified when the storage deletes a file before all of its data was read. */
@FunctionalInterface
public interface DiscardedFileListener {

  /** Why a file was discarded. */
  enum Reason {
    /** The file was older than {@link FileStorageConfiguration#getMaxFileAgeForReadMillis()}. */
    EXPIRED,
    /**
     * The oldest file was removed to stay within {@link
     * FileStorageConfiguration#getMaxFolderSize()}.
     */
    SIZE_LIMIT,
    /** The file contained data that could not be read. */
    CORRUPTED
  }

  /**
   * Called after the file has been deleted.
   *
   * @param file the deleted file.
   * @param reason why the file was deleted.
   */
  void onDiscarded(File file, Reason reason);
}
