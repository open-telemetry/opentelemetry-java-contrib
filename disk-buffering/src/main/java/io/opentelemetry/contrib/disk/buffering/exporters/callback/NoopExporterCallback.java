/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.exporters.callback;

import java.util.Collection;
import javax.annotation.Nullable;

final class NoopExporterCallback<T> implements ExporterCallback<T> {

  NoopExporterCallback() {}

  @Override
  public void onExportSuccess(Collection<T> items) {}

  @Override
  public void onExportError(Collection<T> items, @Nullable Throwable error) {}

  @Override
  public void onShutdown() {}
}
