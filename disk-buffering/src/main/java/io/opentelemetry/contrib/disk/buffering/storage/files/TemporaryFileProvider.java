/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.storage.files;

import java.io.File;
import java.io.IOException;

/** Provides a temporary file needed to do the disk reading process. */
public interface TemporaryFileProvider {

  /**
   * Creates a temporary file.
   *
   * @param prefix The prefix for the provided file name.
   */
  File createTemporaryFile(String prefix) throws IOException;
}
