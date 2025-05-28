/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.storage.files.reader;

import io.opentelemetry.contrib.disk.buffering.internal.storage.files.utils.FileStream;
import io.opentelemetry.contrib.disk.buffering.internal.utils.ProtobufTools;
import java.io.IOException;
import javax.annotation.Nullable;

public final class DelimitedProtoStreamReader implements StreamReader {
  private final FileStream fileStream;

  public DelimitedProtoStreamReader(FileStream fileStream) {
    this.fileStream = fileStream;
  }

  @Override
  @Nullable
  public ReadResult read() throws IOException {
    long startingPosition = fileStream.getPosition();
    int itemSize = getNextItemSize();
    if (itemSize < 1) {
      return null;
    }
    byte[] bytes = new byte[itemSize];
    if (fileStream.read(bytes) < 0) {
      return null;
    }
    return new ReadResult(bytes, fileStream.getPosition() - startingPosition);
  }

  private int getNextItemSize() {
    try {
      int firstByte = fileStream.read();
      if (firstByte == -1) {
        return 0;
      }
      return ProtobufTools.readRawVarint32(firstByte, fileStream);
    } catch (IOException e) {
      return 0;
    }
  }

  @Override
  public void close() throws IOException {
    fileStream.close();
  }

  public static class Factory implements StreamReader.Factory {

    private static final Factory INSTANCE = new DelimitedProtoStreamReader.Factory();

    public static Factory getInstance() {
      return INSTANCE;
    }

    private Factory() {}

    @Override
    public StreamReader create(FileStream fileStream) {
      return new DelimitedProtoStreamReader(fileStream);
    }
  }
}
