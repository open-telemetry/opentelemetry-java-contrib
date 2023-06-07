package io.opentelemetry.contrib.disk.buffering.internal.storage.files;

import java.io.File;

public final class ReadableFile extends FileHolder {
  public ReadableFile(File file) {
    super(file);
  }

  @Override
  public long getSize() {
    return file.length();
  }

  @Override
  public void close() {}
}
