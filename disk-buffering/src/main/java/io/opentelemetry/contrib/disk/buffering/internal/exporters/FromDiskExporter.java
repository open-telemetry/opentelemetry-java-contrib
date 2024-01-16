/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.exporters;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * This interface is for classes that are able to read telemetry previously buffered
 * on disk and send it to another delegated exporter.
 */
public interface FromDiskExporter {

  /**
   * Reads data from the disk and attempts to export it.
   *
   * @param timeout The amount of time to wait for the wrapped exporter to finish.
   * @param unit The unit of the time provided.
   * @return true if there was data available and it was successfully exported within the timeout
   *     provided. false otherwise.
   * @throws IOException If an unexpected error happens.
   */
  boolean exportStoredBatch(long timeout, TimeUnit unit) throws IOException;
}
