/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.exporter;

import static java.util.Collections.emptyList;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.opentelemetry.contrib.disk.buffering.StorageConfiguration;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.deserializers.SignalDeserializer;
import io.opentelemetry.contrib.disk.buffering.internal.storage.Storage;
import io.opentelemetry.contrib.disk.buffering.internal.storage.StorageBuilder;
import io.opentelemetry.sdk.common.Clock;
import io.opentelemetry.sdk.common.CompletableResultCode;
import java.io.IOException;
import java.util.Collection;
import java.util.function.Function;
import org.jetbrains.annotations.NotNull;

public class FromDiskExporterBuilder<T> {

  private SignalDeserializer<T> serializer = noopDeserializer();
  private Function<Collection<T>, CompletableResultCode> exportFunction =
      x -> CompletableResultCode.ofFailure();

  private boolean debugEnabled = false;

  @NotNull
  private static <T> SignalDeserializer<T> noopDeserializer() {
    return x -> emptyList();
  }

  private final StorageBuilder storageBuilder = Storage.builder();

  @CanIgnoreReturnValue
  public FromDiskExporterBuilder<T> setFolderName(String folderName) {
    storageBuilder.setFolderName(folderName);
    return this;
  }

  @CanIgnoreReturnValue
  public FromDiskExporterBuilder<T> setStorageConfiguration(StorageConfiguration configuration) {
    storageBuilder.setStorageConfiguration(configuration);
    return this;
  }

  @CanIgnoreReturnValue
  public FromDiskExporterBuilder<T> setStorageClock(Clock clock) {
    storageBuilder.setStorageClock(clock);
    return this;
  }

  @CanIgnoreReturnValue
  public FromDiskExporterBuilder<T> setDeserializer(SignalDeserializer<T> serializer) {
    this.serializer = serializer;
    return this;
  }

  @CanIgnoreReturnValue
  public FromDiskExporterBuilder<T> setExportFunction(
      Function<Collection<T>, CompletableResultCode> exportFunction) {
    this.exportFunction = exportFunction;
    return this;
  }

  @CanIgnoreReturnValue
  public FromDiskExporterBuilder<T> enableDebug() {
    return setDebugEnabled(true);
  }

  @CanIgnoreReturnValue
  public FromDiskExporterBuilder<T> setDebugEnabled(boolean debugEnabled) {
    this.debugEnabled = debugEnabled;
    return this;
  }

  public FromDiskExporterImpl<T> build() throws IOException {
    Storage storage = storageBuilder.build();
    return new FromDiskExporterImpl<>(serializer, exportFunction, storage, debugEnabled);
  }
}
