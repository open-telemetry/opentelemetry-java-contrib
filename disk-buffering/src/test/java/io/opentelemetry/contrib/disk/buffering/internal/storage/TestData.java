/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.storage;

import io.opentelemetry.contrib.disk.buffering.config.StorageConfiguration;
import java.io.File;

public final class TestData {

  public static final long MAX_FILE_AGE_FOR_WRITE_MILLIS = 1000;
  public static final long MIN_FILE_AGE_FOR_READ_MILLIS = MAX_FILE_AGE_FOR_WRITE_MILLIS + 500;
  public static final long MAX_FILE_AGE_FOR_READ_MILLIS = 10_000;
  public static final int MAX_FILE_SIZE = 100;
  public static final int MAX_FOLDER_SIZE = 300;

  public static StorageConfiguration getConfiguration(File rootDir) {
    return StorageConfiguration.builder()
        .setRootDir(rootDir)
        .setMaxFileAgeForWriteMillis(MAX_FILE_AGE_FOR_WRITE_MILLIS)
        .setMinFileAgeForReadMillis(MIN_FILE_AGE_FOR_READ_MILLIS)
        .setMaxFileAgeForReadMillis(MAX_FILE_AGE_FOR_READ_MILLIS)
        .setMaxFileSize(MAX_FILE_SIZE)
        .setMaxFolderSize(MAX_FOLDER_SIZE)
        .build();
  }

  private TestData() {}
}
