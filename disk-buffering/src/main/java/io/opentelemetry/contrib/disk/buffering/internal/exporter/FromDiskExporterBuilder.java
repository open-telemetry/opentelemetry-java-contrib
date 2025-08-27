/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.exporter;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.deserializers.SignalDeserializer;
import io.opentelemetry.contrib.disk.buffering.internal.storage.Storage;
import io.opentelemetry.contrib.disk.buffering.internal.utils.DebugLogger;
import io.opentelemetry.contrib.disk.buffering.internal.utils.SignalTypes;
import io.opentelemetry.exporter.internal.grpc.GrpcExporter;
import io.opentelemetry.exporter.internal.http.HttpExporter;
import io.opentelemetry.exporter.internal.marshal.Marshaler;
import io.opentelemetry.sdk.common.CompletableResultCode;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.logging.Logger;

public class FromDiskExporterBuilder<EXPORT_DATA> {

  private final DebugLogger logger;
  private final Storage storage;
  private final SignalTypes signalType;

  private BiFunction<ProtoByteArrayMarshaler, Integer, CompletableResultCode> exportFunction =
      (x, i) -> CompletableResultCode.ofFailure();

  public FromDiskExporterBuilder(Storage storage, SignalTypes signalType) {
    if (storage == null) {
      throw new NullPointerException("Storage cannot be null");
    }
    this.storage = storage;
    this.signalType = signalType;
    this.logger =
        DebugLogger.wrap(
            Logger.getLogger(FromDiskExporterImpl.class.getName()), storage.isDebugEnabled());
  }

  @CanIgnoreReturnValue
  public FromDiskExporterBuilder<EXPORT_DATA> setExportFunction(
      Function<Collection<EXPORT_DATA>, CompletableResultCode> exportFunction,
      SignalDeserializer<EXPORT_DATA> deserializer) {
    if (!deserializer.signalType().equals(signalType.name())) {
      throw new IllegalArgumentException(
          deserializer.signalType() + " does not match " + signalType);
    }
    this.exportFunction =
        (exportRequest, itemCount) -> {
          try {
            List<EXPORT_DATA> telemetry = deserializer.deserialize(exportRequest.getBytes());
            return exportFunction.apply(telemetry);
          } catch (IOException e) {
            return CompletableResultCode.ofExceptionalFailure(e);
          }
        };
    return this;
  }

  /**
   * The provided HttpExporter should _NOT_ be configured to send JSON. The data is serialized to
   * disk in protobuf format and sent directly to the provided exporter as a serialized payload.
   */
  @CanIgnoreReturnValue
  public FromDiskExporterBuilder<EXPORT_DATA> setExporter(HttpExporter<Marshaler> exporter) {
    // Any way we can assert the exporter is not configured for JSON?
    this.exportFunction = exporter::export;
    return this;
  }

  @CanIgnoreReturnValue
  public FromDiskExporterBuilder<EXPORT_DATA> setExporter(GrpcExporter<Marshaler> exporter) {
    this.exportFunction = exporter::export;
    return this;
  }

  public FromDiskExporterImpl build() throws IOException {
    return new FromDiskExporterImpl(exportFunction, storage, signalType);
  }
}
