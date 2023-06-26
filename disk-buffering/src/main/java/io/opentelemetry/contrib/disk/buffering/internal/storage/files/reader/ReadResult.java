package io.opentelemetry.contrib.disk.buffering.internal.storage.files.reader;

public final class ReadResult {
  public final byte[] content;
  public final int length;

  public ReadResult(byte[] content, int length) {
    this.content = content;
    this.length = length;
  }
}
