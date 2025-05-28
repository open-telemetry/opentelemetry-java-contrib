/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.exporter;

import static java.util.Collections.emptyList;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.deserializers.SignalDeserializer;
import io.opentelemetry.contrib.disk.buffering.internal.storage.Storage;
import io.opentelemetry.sdk.common.CompletableResultCode;
import java.io.IOException;
import java.util.Collection;
import java.util.function.Function;
import org.jetbrains.annotations.NotNull;

public class FromDiskExporterBuilder<T> {

  private SignalDeserializer<T> serializer = noopDeserializer();
  private final Storage storage;

  private Function<Collection<T>, CompletableResultCode> exportFunction =
      x -> CompletableResultCode.ofFailure();

  public FromDiskExporterBuilder(Storage storage) {
    if (storage == null) {
      throw new NullPointerException("Storage cannot be null");
    }
    this.storage = storage;
  }

  @NotNull
  private static <T> SignalDeserializer<T> noopDeserializer() {
    return x -> emptyList();
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

  public FromDiskExporterImpl<T> build() throws IOException {
    return new FromDiskExporterImpl<>(serializer, exportFunction, storage);
  }
}
