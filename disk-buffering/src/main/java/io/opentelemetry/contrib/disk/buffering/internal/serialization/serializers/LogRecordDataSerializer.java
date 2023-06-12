/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.serialization.serializers;

import io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.logs.ResourceLogsDataMapper;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.models.logs.ResourceLogsData;
import io.opentelemetry.sdk.logs.data.LogRecordData;
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
    try {
      return JsonSerializer.serialize(ResourceLogsDataMapper.INSTANCE.toJsonDto(logRecordData));
    } catch (IOException e) {
      throw new IllegalArgumentException(e);
    }
  }

  @Override
  public List<LogRecordData> deserialize(byte[] source) {
    try {
      return ResourceLogsDataMapper.INSTANCE.fromJsonDto(
          JsonSerializer.deserialize(ResourceLogsData.class, source));
    } catch (IOException e) {
      throw new IllegalArgumentException(e);
    }
  }
}
