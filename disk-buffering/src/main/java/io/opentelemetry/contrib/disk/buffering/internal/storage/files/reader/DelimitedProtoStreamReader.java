/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.storage.files.reader;

import io.opentelemetry.contrib.disk.buffering.internal.storage.files.utils.CountingInputStream;
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
      return readRawVarint32(firstByte);
    } catch (IOException e) {
      return 0;
    }
  }

  private int readRawVarint32(int firstByte) throws IOException {
    if ((firstByte & 0x80) == 0) {
      return firstByte;
    }

    int result = firstByte & 0x7f;
    int offset = 7;
    for (; offset < 32; offset += 7) {
      int b = inputStream.read();
      if (b == -1) {
        throw new IllegalStateException();
      }
      result |= (b & 0x7f) << offset;
      if ((b & 0x80) == 0) {
        return result;
      }
    }
    // Keep reading up to 64 bits.
    for (; offset < 64; offset += 7) {
      int b = inputStream.read();
      if (b == -1) {
        throw new IllegalStateException();
      }
      if ((b & 0x80) == 0) {
        return result;
      }
    }
    throw new IllegalStateException();
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
