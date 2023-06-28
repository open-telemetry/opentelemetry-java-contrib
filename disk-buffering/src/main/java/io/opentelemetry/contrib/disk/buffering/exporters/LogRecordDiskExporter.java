/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.exporters;

import io.opentelemetry.contrib.disk.buffering.internal.exporters.DiskExporter;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.serializers.SignalSerializer;
import io.opentelemetry.contrib.disk.buffering.storage.StorageConfiguration;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.logs.LogRecordProcessor;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * This is a {@link LogRecordExporter} wrapper that takes care of intercepting all the signals sent
 * out to be exported, tries to store them in the disk in order to export them later.
 *
 * <p>In order to use it, you need to wrap your own {@link LogRecordExporter} with a new instance of
 * this one, which will be the one you need to register in your {@link LogRecordProcessor}.
 */
public final class LogRecordDiskExporter implements LogRecordExporter, StoredBatchExporter {
  private final LogRecordExporter wrapped;
  private final DiskExporter<LogRecordData> diskExporter;

  /**
   * @param wrapped - Your own exporter.
   * @param rootDir - The directory to create this signal's cache dir where all the data will be
   *     written into.
   * @param configuration - How you want to manage the storage process.
   */
  public LogRecordDiskExporter(
      LogRecordExporter wrapped, File rootDir, StorageConfiguration configuration) {
    this.wrapped = wrapped;
    diskExporter =
        new DiskExporter<>(
            rootDir, configuration, "logs", SignalSerializer.ofLogs(), wrapped::export);
  }

  @Override
  public CompletableResultCode export(Collection<LogRecordData> logs) {
    return diskExporter.onExport(logs);
  }

  @Override
  public CompletableResultCode flush() {
    return CompletableResultCode.ofSuccess();
  }

  @Override
  public CompletableResultCode shutdown() {
    try {
      diskExporter.onShutDown();
    } catch (IOException e) {
      return CompletableResultCode.ofFailure();
    } finally {
      wrapped.shutdown();
    }
    return CompletableResultCode.ofSuccess();
  }

  @Override
  public boolean exportStoredBatch(long timeout, TimeUnit unit) throws IOException {
    return diskExporter.exportStoredBatch(timeout, unit);
  }
}
