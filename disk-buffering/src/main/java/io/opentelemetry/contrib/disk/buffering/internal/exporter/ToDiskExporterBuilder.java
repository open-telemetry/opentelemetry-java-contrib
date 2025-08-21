/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.exporter;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.serializers.SignalSerializer;
import io.opentelemetry.contrib.disk.buffering.internal.storage.Storage;
import io.opentelemetry.sdk.common.CompletableResultCode;
import java.util.Collection;
import java.util.function.Function;

public final class ToDiskExporterBuilder<T> {

  private SignalSerializer<T> serializer = new NoopSerializer<T>();

  private final Storage<T> storage;

  private Function<Collection<T>, CompletableResultCode> exportFunction =
      x -> CompletableResultCode.ofFailure();

  ToDiskExporterBuilder(Storage<T> storage) {
    if (storage == null) {
      throw new NullPointerException("Storage cannot be null");
    }
    this.storage = storage;
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

  public ToDiskExporter<T> build() {
    return new ToDiskExporter<>(serializer, exportFunction, storage);
  }
}
