/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.exporters.callback;

import java.util.Collection;
import javax.annotation.Nullable;

public final class NoopExporterCallback<T> implements ExporterCallback<T> {

  @Override
  public void onExportSuccess(Collection<T> items) {}

  @Override
  public void onExportError(Collection<T> items, @Nullable Throwable error) {}

  @Override
  public void onShutdown() {}
}
