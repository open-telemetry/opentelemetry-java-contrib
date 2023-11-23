/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.storage.files.reader;

import io.opentelemetry.contrib.disk.buffering.internal.storage.files.utils.CountingInputStream;
import io.opentelemetry.contrib.disk.buffering.internal.utils.ProtobufTools;
import java.io.IOException;
import java.io.InputStream;
import javax.annotation.Nullable;

public final class DelimitedProtoStreamReader extends StreamReader {
  private final CountingInputStream countingInputStream;

  public DelimitedProtoStreamReader(InputStream inputStream) {
    super(new CountingInputStream(inputStream));
    countingInputStream = (CountingInputStream) this.inputStream;
  }

  @Override
  @Nullable
  public ReadResult read() throws IOException {
    int startingPosition = countingInputStream.getPosition();
    int itemSize = getNextItemSize();
    if (itemSize < 1) {
      return null;
    }
    byte[] bytes = new byte[itemSize];
    if (inputStream.read(bytes) < 0) {
      return null;
    }
    return new ReadResult(bytes, countingInputStream.getPosition() - startingPosition);
  }

  private int getNextItemSize() {
    try {
      int firstByte = inputStream.read();
      if (firstByte == -1) {
        return 0;
      }
      return ProtobufTools.readRawVarint32(firstByte, inputStream);
    } catch (IOException e) {
      return 0;
    }
  }

  public static class Factory implements StreamReader.Factory {

    private static final Factory INSTANCE = new DelimitedProtoStreamReader.Factory();

    public static Factory getInstance() {
      return INSTANCE;
    }

    private Factory() {}

    @Override
    public StreamReader create(InputStream stream) {
      return new DelimitedProtoStreamReader(stream);
    }
  }
}
