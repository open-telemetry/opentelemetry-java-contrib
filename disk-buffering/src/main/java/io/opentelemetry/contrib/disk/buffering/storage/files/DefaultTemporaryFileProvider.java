package io.opentelemetry.contrib.disk.buffering.storage.files;

import java.io.File;
import java.io.IOException;

public class DefaultTemporaryFileProvider implements TemporaryFileProvider {
  public static final TemporaryFileProvider INSTANCE = new DefaultTemporaryFileProvider();

  private DefaultTemporaryFileProvider() {}

  @Override
  public File createTemporaryFile(String prefix) throws IOException {
    return File.createTempFile(prefix + "_", ".tmp");
  }
}
