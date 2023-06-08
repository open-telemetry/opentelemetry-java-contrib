package io.opentelemetry.contrib.disk.buffering.internal.storage.files.utils;

import java.io.File;
import java.io.IOException;

public class TemporaryFileProvider {
  public static final TemporaryFileProvider INSTANCE = new TemporaryFileProvider();

  private TemporaryFileProvider() {}

  public File createTemporaryFile() throws IOException {
    return File.createTempFile("", "");
  }
}
