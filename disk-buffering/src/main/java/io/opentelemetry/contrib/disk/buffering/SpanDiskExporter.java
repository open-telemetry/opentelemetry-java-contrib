/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering;

import io.opentelemetry.contrib.disk.buffering.internal.StorageConfiguration;
import io.opentelemetry.contrib.disk.buffering.internal.exporters.DiskExporter;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.serializers.SignalSerializer;
import io.opentelemetry.contrib.disk.buffering.internal.storage.utils.StorageClock;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * This is a {@link SpanExporter} wrapper that takes care of intercepting all the signals sent out
 * to be exported, tries to store them in the disk in order to export them later.
 *
 * <p>In order to use it, you need to wrap your own {@link SpanExporter} with a new instance of this
 * one, which will be the one you need to register in your {@link SpanProcessor}.
 */
public final class SpanDiskExporter implements SpanExporter, StoredBatchExporter {
  private final SpanExporter wrapped;
  private final DiskExporter<SpanData> diskExporter;

  /**
   * Creates a new instance of {@link SpanDiskExporter}.
   *
   * @param wrapped - The exporter where the data retrieved from the disk will be delegated to.
   * @param rootDir - The directory to create this signal's cache dir where all the data will be
   *     written into.
   * @param configuration - How you want to manage the storage process.
   * @throws IOException If no dir can be created in rootDir.
   */
  public static SpanDiskExporter create(
      SpanExporter wrapped, File rootDir, StorageConfiguration configuration) throws IOException {
    return create(wrapped, rootDir, configuration, StorageClock.getInstance());
  }

  // This is exposed for testing purposes.
  public static SpanDiskExporter create(
      SpanExporter wrapped, File rootDir, StorageConfiguration configuration, StorageClock clock)
      throws IOException {
    DiskExporter<SpanData> diskExporter =
        DiskExporter.<SpanData>builder()
            .setRootDir(rootDir)
            .setFolderName("spans")
            .setStorageConfiguration(configuration)
            .setSerializer(SignalSerializer.ofSpans())
            .setExportFunction(wrapped::export)
            .setStorageClock(clock)
            .build();
    return new SpanDiskExporter(wrapped, diskExporter);
  }

  private SpanDiskExporter(SpanExporter wrapped, DiskExporter<SpanData> diskExporter)
      throws IOException {
    this.wrapped = wrapped;
    this.diskExporter = diskExporter;
  }

  @Override
  public CompletableResultCode export(Collection<SpanData> spans) {
    return diskExporter.onExport(spans);
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
  public CompletableResultCode flush() {
    return CompletableResultCode.ofSuccess();
  }

  @Override
  public boolean exportStoredBatch(long timeout, TimeUnit unit) throws IOException {
    return diskExporter.exportStoredBatch(timeout, unit);
  }
}
