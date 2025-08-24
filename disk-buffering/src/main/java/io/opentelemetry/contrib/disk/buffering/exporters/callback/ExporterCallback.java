/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.exporters.callback;

import io.opentelemetry.contrib.disk.buffering.SignalType;
import javax.annotation.Nullable;

/** Notifies about exporter and storage-related operations from within a signal to disk exporter. */
public interface ExporterCallback {
  /**
   * Called when an export to disk operation succeeded.
   *
   * @param type The type of signal associated to the exporter.
   */
  void onExportSuccess(SignalType type);

  /**
   * Called when an export to disk operation failed.
   *
   * @param type The type of signal associated to the exporter.
   * @param error Optional - provides more information of why the operation failed.
   */
  void onExportError(SignalType type, @Nullable Throwable error);

  /**
   * Called when the exporter is closed.
   *
   * @param type The type of signal associated to the exporter.
   */
  void onShutdown(SignalType type);

  static ExporterCallback noop() {
    return NoopExporterCallback.INSTANCE;
  }
}
