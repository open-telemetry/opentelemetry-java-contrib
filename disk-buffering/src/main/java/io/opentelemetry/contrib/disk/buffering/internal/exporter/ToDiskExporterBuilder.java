/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.exporter;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.opentelemetry.contrib.disk.buffering.internal.StorageConfiguration;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.serializers.SignalSerializer;
import io.opentelemetry.contrib.disk.buffering.internal.storage.Storage;
import io.opentelemetry.contrib.disk.buffering.internal.storage.StorageBuilder;
import io.opentelemetry.sdk.common.Clock;
import io.opentelemetry.sdk.common.CompletableResultCode;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public final class ToDiskExporterBuilder<T> {

  private SignalSerializer<T> serializer =
      new SignalSerializer<T>() {

        @Override
        public byte[] serialize(Collection<T> ts) {
          return new byte[0];
        }

        @Override
        public List<T> deserialize(byte[] source) {
          return Collections.emptyList();
        }
      };

  private final StorageBuilder storageBuilder = Storage.builder();

  private Function<Collection<T>, CompletableResultCode> exportFunction =
      x -> CompletableResultCode.ofFailure();

  ToDiskExporterBuilder() {}

//  @CanIgnoreReturnValue
//  public ToDiskExporterBuilder<T> setRootDir(File rootDir) {
//    storageBuilder.setRootDir(rootDir);
//    return this;
//  }

  @CanIgnoreReturnValue
  public ToDiskExporterBuilder<T> setFolderName(String folderName) {
    storageBuilder.setFolderName(folderName);
    return this;
  }

  @CanIgnoreReturnValue
  public ToDiskExporterBuilder<T> setStorageConfiguration(StorageConfiguration configuration) {
    validateConfiguration(configuration);
    storageBuilder.setStorageConfiguration(configuration);
    return this;
  }

  @CanIgnoreReturnValue
  public ToDiskExporterBuilder<T> setStorageClock(Clock clock) {
    storageBuilder.setStorageClock(clock);
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
    Storage storage = storageBuilder.build();
    return new ToDiskExporter<>(serializer, exportFunction, storage);
  }

  private static void validateConfiguration(StorageConfiguration configuration) {
    if (configuration.getMinFileAgeForReadMillis() <= configuration.getMaxFileAgeForWriteMillis()) {
      throw new IllegalArgumentException(
          "The configured max file age for writing must be lower than the configured min file age for reading");
    }
  }
}
