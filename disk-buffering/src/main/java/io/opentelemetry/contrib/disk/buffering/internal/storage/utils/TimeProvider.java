package io.opentelemetry.contrib.disk.buffering.internal.storage.utils;

public class TimeProvider {
  public long getSystemCurrentTimeMillis() {
    return System.currentTimeMillis();
  }
}
