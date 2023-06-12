/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.exporters;

import io.opentelemetry.contrib.disk.buffering.internal.exporters.AbstractDiskExporter;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.serializers.SignalSerializer;
import io.opentelemetry.contrib.disk.buffering.storage.StorageConfiguration;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.io.File;
import java.io.IOException;
import java.util.Collection;

/**
 * This is a {@link SpanExporter} wrapper that takes care of intercepting all the signals sent out
 * to be exported, tries to store them in the disk in order to export them later.
 *
 * <p>In order to use it, you need to wrap your own {@link SpanExporter} with a new instance of this
 * one, which will be the one you need to register in your {@link SpanProcessor}.
 */
public final class SpanDiskExporter extends AbstractDiskExporter<SpanData> implements SpanExporter {
  private final SpanExporter wrapped;

  /**
   * @param wrapped - Your own exporter.
   * @param rootDir - The directory to create this signal's cache dir where all the data will be
   *     written into.
   * @param configuration - How you want to manage the storage process.
   */
  public SpanDiskExporter(SpanExporter wrapped, File rootDir, StorageConfiguration configuration) {
    super(rootDir, configuration);
    this.wrapped = wrapped;
  }

  @Override
  public CompletableResultCode export(Collection<SpanData> spans) {
    return onExport(spans);
  }

  @Override
  public CompletableResultCode shutdown() {
    try {
      onShutDown();
    } catch (IOException e) {
      return CompletableResultCode.ofFailure();
    } finally {
      wrapped.shutdown();
    }
    return CompletableResultCode.ofSuccess();
  }

  @Override
  protected String getStorageFolderName() {
    return "spans";
  }

  @Override
  protected CompletableResultCode doExport(Collection<SpanData> data) {
    return wrapped.export(data);
  }

  @Override
  protected SignalSerializer<SpanData> getSerializer() {
    return SignalSerializer.ofSpans();
  }

  @Override
  public CompletableResultCode flush() {
    return wrapped.flush();
  }
}
