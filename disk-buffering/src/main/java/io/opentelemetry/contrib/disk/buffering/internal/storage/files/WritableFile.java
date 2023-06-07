package io.opentelemetry.contrib.disk.buffering.internal.storage.files;

import java.io.File;
import java.io.IOException;

public class WritableFile extends FileHolder {
  public WritableFile(File file) {
    super(file);
  }

  @Override
  public void close() throws IOException {}
}
