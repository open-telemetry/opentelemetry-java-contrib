/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.exporter;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public interface FromDiskExporter {
  boolean exportStoredBatch(long timeout, TimeUnit unit) throws IOException;

  void shutdown() throws IOException;
}
