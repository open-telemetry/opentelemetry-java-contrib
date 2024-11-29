/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.serialization.deserializers;

import io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.logs.ProtoLogsDataMapper;
import io.opentelemetry.contrib.disk.buffering.internal.utils.SignalTypes;
import io.opentelemetry.proto.logs.v1.LogsData;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import java.io.IOException;
import java.util.List;

public final class LogRecordDataDeserializer implements SignalDeserializer<LogRecordData> {
  private static final LogRecordDataDeserializer INSTANCE = new LogRecordDataDeserializer();

  private LogRecordDataDeserializer() {}

  static LogRecordDataDeserializer getInstance() {
    return INSTANCE;
  }

  @Override
  public List<LogRecordData> deserialize(byte[] source) throws DeserializationException {
    try {
      return ProtoLogsDataMapper.getInstance().fromProto(LogsData.ADAPTER.decode(source));
    } catch (IOException e) {
      throw new DeserializationException(e);
    }
  }

  @Override
  public String signalType() {
    return SignalTypes.logs.name();
  }
}
