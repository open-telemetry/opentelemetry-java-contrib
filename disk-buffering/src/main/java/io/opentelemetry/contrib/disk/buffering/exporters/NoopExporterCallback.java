/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.exporters;

import io.opentelemetry.contrib.disk.buffering.SignalType;
import javax.annotation.Nullable;

final class NoopExporterCallback implements ExporterCallback {
  static final NoopExporterCallback INSTANCE = new NoopExporterCallback();

  private NoopExporterCallback() {}

  @Override
  public void onExportSuccess(SignalType type) {}

  @Override
  public void onExportError(SignalType type, @Nullable Throwable error) {}

  @Override
  public void onShutdown(SignalType type) {}
}
