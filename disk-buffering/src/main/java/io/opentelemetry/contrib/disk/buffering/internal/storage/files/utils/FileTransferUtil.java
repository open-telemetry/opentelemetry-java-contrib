/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.storage.files.utils;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

public final class FileTransferUtil implements Closeable {
  private final File output;

  private final FileChannel inputChannel;

  public FileTransferUtil(FileInputStream input, File output) {
    this.output = output;
    inputChannel = input.getChannel();
  }

  public void transferBytes(int offset, int length) throws IOException {
    try (FileOutputStream out = new FileOutputStream(output, false)) {
      inputChannel.transferTo(offset, length, out.getChannel());
    }
  }

  @Override
  public void close() throws IOException {
    inputChannel.close();
  }
}
