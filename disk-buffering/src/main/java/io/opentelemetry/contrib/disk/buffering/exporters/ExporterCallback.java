package io.opentelemetry.contrib.disk.buffering.exporters;

import io.opentelemetry.contrib.disk.buffering.SignalType;
import javax.annotation.Nullable;

public interface ExporterCallback {
  void onExportSuccess(SignalType type);

  void onExportError(SignalType type, @Nullable Throwable error);

  void onShutdown(SignalType type);

  static ExporterCallback noop() {
    return NoopExporterCallback.INSTANCE;
  }
}
