package io.opentelemetry.contrib.disk.buffering.internal.storage.files;

import java.io.Closeable;
import java.io.File;

public abstract class FileHolder implements Closeable {
  public final File file;

  public FileHolder(File file) {
    this.file = file;
  }
}
