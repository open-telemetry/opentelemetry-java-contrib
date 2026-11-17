/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.storage.files;

import java.io.Closeable;
import java.io.File;

/**
 * Common operations for storage files.
 *
 * <p>This class is internal and is hence not for public use. Its APIs are unstable and can change
 * at any time.
 */
public interface FileOperations extends Closeable {
  boolean hasExpired();

  boolean isClosed();

  File getFile();
}
