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

public final class LogRecordDataSerializer implements SignalSerializer<LogRecordData> {
  private static final LogRecordDataSerializer INSTANCE = new LogRecordDataSerializer();

  private LogRecordDataSerializer() {}

  static LogRecordDataSerializer getInstance() {
    return INSTANCE;
  }

  @Override
  public byte[] serialize(Collection<LogRecordData> logRecordData) {
    LogsData proto = ProtoLogsDataMapper.getInstance().toProto(logRecordData);
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
      return ProtoLogsDataMapper.getInstance().fromProto(LogsData.parseFrom(source));
    } catch (InvalidProtocolBufferException e) {
      throw new IllegalArgumentException(e);
    }
  }
}
