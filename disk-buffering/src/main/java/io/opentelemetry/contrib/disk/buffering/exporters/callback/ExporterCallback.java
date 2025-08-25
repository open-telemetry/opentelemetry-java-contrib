/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.exporters.callback;

import java.util.Collection;
import javax.annotation.Nullable;

/** Notifies about exporter and storage-related operations from within a signal to disk exporter. */
public interface ExporterCallback<T> {
  /**
   * Called when an export to disk operation succeeded.
   *
   * @param items The items successfully stored in disk.
   */
  void onExportSuccess(Collection<T> items);

  /**
   * Called when an export to disk operation failed.
   *
   * @param items The items that couldn't get stored in disk.
   * @param error Optional - provides more information of why the operation failed.
   */
  void onExportError(Collection<T> items, @Nullable Throwable error);

  /** Called when the exporter is closed. */
  void onShutdown();

  static <T> ExporterCallback<T> noop() {
    return new NoopExporterCallback<>();
  }
}
