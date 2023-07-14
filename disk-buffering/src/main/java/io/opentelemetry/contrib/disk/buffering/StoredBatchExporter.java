/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public interface StoredBatchExporter {

  /**
   * Reads data from the disk and attempts to export it.
   *
   * @param timeout The amount of time to wait for the wrapped exporter to finish.
   * @param unit The unit of the time provided.
   * @return TRUE if there was data available and it was successfully exported within the timeout
   *     provided. FALSE if either of those conditions didn't meet.
   * @throws IOException If an unexpected error happens.
   */
  boolean exportStoredBatch(long timeout, TimeUnit unit) throws IOException;
}
