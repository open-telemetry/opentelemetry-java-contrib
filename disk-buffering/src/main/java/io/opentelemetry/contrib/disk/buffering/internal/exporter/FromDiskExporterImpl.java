/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.exporter;

import io.opentelemetry.contrib.disk.buffering.internal.serialization.deserializers.DeserializationException;
import io.opentelemetry.contrib.disk.buffering.internal.storage.Storage;
import io.opentelemetry.contrib.disk.buffering.internal.storage.files.reader.ProcessResult;
import io.opentelemetry.contrib.disk.buffering.internal.storage.responses.ReadableResult;
import io.opentelemetry.contrib.disk.buffering.internal.utils.DebugLogger;
import io.opentelemetry.contrib.disk.buffering.internal.utils.ProtobufTools;
import io.opentelemetry.contrib.disk.buffering.internal.utils.SignalTypes;
import io.opentelemetry.exporter.internal.marshal.ProtoFieldInfo;
import io.opentelemetry.proto.collector.logs.v1.internal.ExportLogsServiceRequest;
import io.opentelemetry.proto.collector.metrics.v1.internal.ExportMetricsServiceRequest;
import io.opentelemetry.proto.collector.trace.v1.internal.ExportTraceServiceRequest;
import io.opentelemetry.sdk.common.CompletableResultCode;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.logging.Logger;

/**
 * Signal-type generic class that can read telemetry previously buffered on disk and send it to
 * another delegated exporter.
 */
public final class FromDiskExporterImpl implements FromDiskExporter {
  private final DebugLogger logger;
  private final Storage storage;
  private final SignalTypes signalType;
  private final BiFunction<ProtoByteArrayMarshaler, Integer, CompletableResultCode> exportFunction;

  FromDiskExporterImpl(
      BiFunction<ProtoByteArrayMarshaler, Integer, CompletableResultCode> exportFunction,
      Storage storage,
      SignalTypes signalType) {
    this.exportFunction = exportFunction;
    this.storage = storage;
    this.signalType = signalType;
    this.logger =
        DebugLogger.wrap(
            Logger.getLogger(FromDiskExporterImpl.class.getName()), storage.isDebugEnabled());
  }

  public static <T> FromDiskExporterBuilder<T> builder(Storage storage, SignalTypes signalType) {
    return new FromDiskExporterBuilder<>(storage, signalType);
  }

  /**
   * Reads data from the disk and attempts to export it.
   *
   * @param timeout The amount of time to wait for the wrapped exporter to finish.
   * @param unit The unit of the time provided.
   * @return true if there was data available, and it was successfully exported within the timeout
   *     provided. false otherwise.
   * @throws IOException If an unexpected error happens.
   */
  @Override
  public boolean exportStoredBatch(long timeout, TimeUnit unit) throws IOException {
    logger.log("Attempting to export " + signalType.name() + " batch from disk.");
    ReadableResult result =
        storage.readAndProcess(
            bytes -> {
              logger.log("Read " + bytes.length + " " + signalType.name() + " bytes from storage.");
              ProtoFieldInfo field;
              switch (signalType) {
                case metrics:
                  field = ExportMetricsServiceRequest.RESOURCE_METRICS;
                  break;
                case logs:
                  field = ExportLogsServiceRequest.RESOURCE_LOGS;
                  break;
                case spans:
                  field = ExportTraceServiceRequest.RESOURCE_SPANS;
                  break;
                default:
                  throw new IllegalStateException("Unsupported signal type: " + signalType);
              }
              int itemCount = 0;
              try {
                itemCount = ProtobufTools.countRepeatedField(bytes, field.getFieldNumber());
              } catch (DeserializationException e) {
                return ProcessResult.CONTENT_INVALID;
              }
              logger.log("Now exporting batch of " + itemCount + " " + signalType.name());
              CompletableResultCode join =
                  exportFunction
                      .apply(new ProtoByteArrayMarshaler(bytes), itemCount)
                      .join(timeout, unit);
              return join.isSuccess() ? ProcessResult.SUCCEEDED : ProcessResult.TRY_LATER;
            });
    return result == ReadableResult.SUCCEEDED;
  }

  @Override
  public void shutdown() throws IOException {
    storage.close();
  }
}
