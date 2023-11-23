/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.storage.files;

import java.io.Closeable;
import java.io.File;

public interface FileOperations extends Closeable {
  long getSize();

  boolean hasExpired();

  boolean isClosed();

  File getFile();

  default String getFileName() {
    return getFile().getName();
  }
}
