package io.opentelemetry.contrib.disk.buffering.internal.storage.files.reader;

/** Result of processing the contents of a file. */
public enum ProcessResult {
  SUCCEEDED,
  TRY_LATER,
  CONTENT_INVALID
}
