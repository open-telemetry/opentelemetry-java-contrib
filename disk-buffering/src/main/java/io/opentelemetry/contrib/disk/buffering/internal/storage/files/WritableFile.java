package io.opentelemetry.contrib.disk.buffering.internal.storage.files;

import java.io.File;

public final class WritableFile extends FileHolder {
  public WritableFile(File file) {
    super(file);
  }

  @Override
  public void close() {}
}
