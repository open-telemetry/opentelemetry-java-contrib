/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.exporter;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.opentelemetry.contrib.disk.buffering.config.StorageConfiguration;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.serializers.SignalSerializer;
import io.opentelemetry.contrib.disk.buffering.internal.storage.Storage;
import io.opentelemetry.contrib.disk.buffering.internal.storage.StorageBuilder;
import io.opentelemetry.sdk.common.Clock;
import io.opentelemetry.sdk.common.CompletableResultCode;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Collection;
import java.util.function.Function;

public final class ToDiskExporterBuilder<T> {

  private SignalSerializer<T> serializer = ts -> new byte[0];

  @Deprecated
  private final StorageBuilder storageBuilder = Storage.builder();

  @Nullable
  private Storage storage = null;

  private Function<Collection<T>, CompletableResultCode> exportFunction =
      x -> CompletableResultCode.ofFailure();
  private boolean debugEnabled = false;

  @Deprecated
  ToDiskExporterBuilder() {}

  ToDiskExporterBuilder(Storage storage) {
    if (storage == null) {
      throw new NullPointerException("Storage cannot be null");
    }
    this.storage = storage;
  }

  @CanIgnoreReturnValue
  public ToDiskExporterBuilder<T> enableDebug() {
    return setDebugEnabled(true);
  }

  @CanIgnoreReturnValue
  public ToDiskExporterBuilder<T> setDebugEnabled(boolean debugEnabled) {
    this.debugEnabled = debugEnabled;
    return this;
  }

  @Deprecated
  @CanIgnoreReturnValue
  public ToDiskExporterBuilder<T> setFolderName(String folderName) {
    storageBuilder.setFolderName(folderName);
    return this;
  }

  @Deprecated
  @CanIgnoreReturnValue
  public ToDiskExporterBuilder<T> setStorageConfiguration(StorageConfiguration configuration) {
    validateConfiguration(configuration);
    storageBuilder.setStorageConfiguration(configuration);
    return this;
  }

  @Deprecated
  @CanIgnoreReturnValue
  public ToDiskExporterBuilder<T> setStorageClock(Clock clock) {
    storageBuilder.setStorageClock(clock);
    return this;
  }

  @CanIgnoreReturnValue
  public ToDiskExporterBuilder<T> setStorage(Storage storage) {
    this.storage = storage;
    return this;
  }

  @CanIgnoreReturnValue
  public ToDiskExporterBuilder<T> setSerializer(SignalSerializer<T> serializer) {
    this.serializer = serializer;
    return this;
  }

  @CanIgnoreReturnValue
  public ToDiskExporterBuilder<T> setExportFunction(
      Function<Collection<T>, CompletableResultCode> exportFunction) {
    this.exportFunction = exportFunction;
    return this;
  }

  public ToDiskExporter<T> build() throws IOException {
    Storage storage = this.storage != null ? this.storage : storageBuilder.build();
    return new ToDiskExporter<>(serializer, exportFunction, storage, debugEnabled);
  }

  private static void validateConfiguration(StorageConfiguration configuration) {
    if (configuration.getMinFileAgeForReadMillis() <= configuration.getMaxFileAgeForWriteMillis()) {
      throw new IllegalArgumentException(
          "The configured max file age for writing must be lower than the configured min file age for reading");
    }
  }
}
