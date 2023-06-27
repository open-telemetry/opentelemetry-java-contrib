/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.serialization.serializers;

import com.google.protobuf.InvalidProtocolBufferException;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.logs.ProtoLogsDataMapper;
import io.opentelemetry.proto.logs.v1.LogsData;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;

public final class LogRecordDataSerializer implements SignalSerializer<LogRecordData> {
  @Nullable private static LogRecordDataSerializer instance;

  private LogRecordDataSerializer() {}

  static LogRecordDataSerializer get() {
    if (instance == null) {
      instance = new LogRecordDataSerializer();
    }
    return instance;
  }

  @Override
  public byte[] serialize(Collection<LogRecordData> logRecordData) {
    LogsData proto = ProtoLogsDataMapper.INSTANCE.toProto(logRecordData);
    try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      proto.writeDelimitedTo(out);
      return out.toByteArray();
    } catch (IOException e) {
      throw new IllegalArgumentException(e);
    }
  }

  @Override
  public List<LogRecordData> deserialize(byte[] source) {
    try {
      return ProtoLogsDataMapper.INSTANCE.fromProto(LogsData.parseFrom(source));
    } catch (InvalidProtocolBufferException e) {
      throw new IllegalArgumentException(e);
    }
  }
}
