package io.opentelemetry.contrib.disk.buffering.internal.storage;

import com.google.auto.value.AutoValue;
import java.util.concurrent.TimeUnit;

@AutoValue
public abstract class Configuration {
  public abstract long getMaxFileAgeForWriteMillis();

  public abstract long getMinFileAgeForReadMillis();

  public abstract long getMaxFileAgeForReadMillis();

  public abstract int getMaxFileSize();

  public abstract int getMaxFolderSize();

  public static Configuration getDefault() {
    return builder().build();
  }

  public static Builder builder() {
    return new AutoValue_Configuration.Builder()
        .setMaxFileSize(1024 * 1024) // 1MB
        .setMaxFolderSize(20 * 1024 * 1024) // 20MB
        .setMaxFileAgeForWriteMillis(TimeUnit.SECONDS.toMillis(5))
        .setMinFileAgeForReadMillis(TimeUnit.SECONDS.toMillis(6))
        .setMaxFileAgeForReadMillis(TimeUnit.HOURS.toMillis(18));
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setMaxFileAgeForWriteMillis(long value);

    public abstract Builder setMinFileAgeForReadMillis(long value);

    public abstract Builder setMaxFileAgeForReadMillis(long value);

    public abstract Builder setMaxFileSize(int value);

    public abstract Builder setMaxFolderSize(int value);

    public abstract Configuration build();
  }
}
