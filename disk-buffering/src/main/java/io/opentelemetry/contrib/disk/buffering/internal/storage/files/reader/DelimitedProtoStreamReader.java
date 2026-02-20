/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.storage.files.reader;

import io.opentelemetry.contrib.disk.buffering.internal.utils.ProtobufTools;
import java.io.IOException;
import java.io.InputStream;
import javax.annotation.Nullable;

public final class DelimitedProtoStreamReader implements StreamReader {
  private final InputStream inputStream;

  public DelimitedProtoStreamReader(InputStream inputStream) {
    this.inputStream = inputStream;
  }

  @Override
  @Nullable
  public byte[] readNext() throws IOException {
    int itemSize = getNextItemSize();
    if (itemSize < 1) {
      return null;
    }
    byte[] bytes = new byte[itemSize];
    int offset = 0;
    int readCt;
    do {
      readCt = inputStream.read(bytes, offset, itemSize - offset);
      offset += readCt;
    } while (readCt != -1 && offset < itemSize);
    if (offset != itemSize) {
      throw new IOException("Unable to read the whole item correctly");
    }
    return bytes;
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

  @Override
  public void close() throws IOException {
    inputStream.close();
  }

  public static class Factory implements StreamReader.Factory {

    private static final Factory INSTANCE = new DelimitedProtoStreamReader.Factory();

    public static Factory getInstance() {
      return INSTANCE;
    }

    private Factory() {}

    @Override
    public StreamReader create(InputStream inputStream) {
      return new DelimitedProtoStreamReader(inputStream);
    }
  }
}
