package io.opentelemetry.contrib.disk.buffering.internal.storage.files;

import java.nio.charset.StandardCharsets;

public class Constants {

  public static final byte[] NEW_LINE_BYTES =
      System.lineSeparator().getBytes(StandardCharsets.UTF_8);
  public static final int NEW_LINE_BYTES_SIZE = NEW_LINE_BYTES.length;

  private Constants() {}
}
