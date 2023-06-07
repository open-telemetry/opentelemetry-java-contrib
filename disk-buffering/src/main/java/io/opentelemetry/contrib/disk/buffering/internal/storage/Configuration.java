package io.opentelemetry.contrib.disk.buffering.internal.storage;

public class Configuration {
  public final long maxFileAgeInMillis;

  public Configuration(long maxFileAgeInMillis) {
    this.maxFileAgeInMillis = maxFileAgeInMillis;
  }
}
