/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.storage;

import io.opentelemetry.contrib.disk.buffering.StorageConfiguration;
import io.opentelemetry.contrib.disk.buffering.internal.files.DefaultTemporaryFileProvider;
import io.opentelemetry.contrib.disk.buffering.internal.files.TemporaryFileProvider;
import java.io.File;

public final class TestData {

  public static final long MAX_FILE_AGE_FOR_WRITE_MILLIS = 1000;
  public static final long MIN_FILE_AGE_FOR_READ_MILLIS = MAX_FILE_AGE_FOR_WRITE_MILLIS + 500;
  public static final long MAX_FILE_AGE_FOR_READ_MILLIS = 10_000;
  public static final int MAX_FILE_SIZE = 100;
  public static final int MAX_FOLDER_SIZE = 300;

  public static StorageConfiguration getDefaultConfiguration(File rootDir) {
    TemporaryFileProvider fileProvider = DefaultTemporaryFileProvider.getInstance();
    return getConfiguration(fileProvider, rootDir);
  }

  public static StorageConfiguration getConfiguration(
      TemporaryFileProvider fileProvider, File rootDir) {
    return StorageConfiguration.builder()
        .setRootDir(rootDir)
        .setMaxFileAgeForWriteMillis(MAX_FILE_AGE_FOR_WRITE_MILLIS)
        .setMinFileAgeForReadMillis(MIN_FILE_AGE_FOR_READ_MILLIS)
        .setMaxFileAgeForReadMillis(MAX_FILE_AGE_FOR_READ_MILLIS)
        .setMaxFileSize(MAX_FILE_SIZE)
        .setMaxFolderSize(MAX_FOLDER_SIZE)
        .setTemporaryFileProvider(fileProvider)
        .build();
  }

  private TestData() {}
}
