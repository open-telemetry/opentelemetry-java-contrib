/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.storage.files;

import java.io.Closeable;
import java.io.File;

public abstract class StorageFile implements Closeable {
  public final File file;

  public StorageFile(File file) {
    this.file = file;
  }

  public abstract long getSize();

  public abstract boolean hasExpired();

  public abstract boolean isClosed();
}
