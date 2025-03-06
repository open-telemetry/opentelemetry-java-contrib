/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.files;

import io.opentelemetry.contrib.disk.buffering.configuration.TemporaryFileProvider;
import java.io.File;
import java.io.IOException;

public final class DefaultTemporaryFileProvider implements TemporaryFileProvider {
  private static final TemporaryFileProvider INSTANCE = new DefaultTemporaryFileProvider();

  public static TemporaryFileProvider getInstance() {
    return INSTANCE;
  }

  private DefaultTemporaryFileProvider() {}

  @Override
  public File createTemporaryFile(String prefix) throws IOException {
    return File.createTempFile(prefix + "_", ".tmp");
  }
}
