package io.opentelemetry.contrib.disk.buffering.exporters;

import io.opentelemetry.contrib.disk.buffering.SignalType;
import javax.annotation.Nullable;

public interface ExporterCallback {
  void onExportSuccess(SignalType type);

  void onExportError(SignalType type, @Nullable Throwable error);

  void onShutdown(SignalType type);

  static ExporterCallback noop() {
    return Noop.INSTANCE;
  }

  class Noop implements ExporterCallback {
    private static final Noop INSTANCE = new Noop();

    private Noop() {}

    @Override
    public void onExportSuccess(SignalType type) {}

    @Override
    public void onExportError(SignalType type, @Nullable Throwable error) {}

    @Override
    public void onShutdown(SignalType type) {}
  }
}
