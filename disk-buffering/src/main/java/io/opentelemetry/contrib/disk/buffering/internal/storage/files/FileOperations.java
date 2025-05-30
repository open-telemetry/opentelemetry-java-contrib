/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.storage.files;

import java.io.Closeable;
import java.io.File;

public interface FileOperations extends Closeable {
  boolean hasExpired();

  boolean isClosed();

  File getFile();
}
