/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.storage;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.opentelemetry.contrib.disk.buffering.internal.StorageConfiguration;
import io.opentelemetry.sdk.common.Clock;
import java.io.File;
import java.io.IOException;

public class StorageBuilder {

  private File rootDir = new File(".");
  private String folderName = "data";
  private StorageConfiguration configuration = StorageConfiguration.getDefault();
  private Clock clock = Clock.getDefault();

  StorageBuilder() {}

  @CanIgnoreReturnValue
  public StorageBuilder setFolderName(String folderName) {
    this.folderName = folderName;
    return this;
  }

  @CanIgnoreReturnValue
  public StorageBuilder setRootDir(File rootDir) {
    this.rootDir = rootDir;
    return this;
  }

  @CanIgnoreReturnValue
  public StorageBuilder setStorageConfiguration(StorageConfiguration configuration) {
    this.configuration = configuration;
    return this;
  }

  @CanIgnoreReturnValue
  public StorageBuilder setStorageClock(Clock clock) {
    this.clock = clock;
    return this;
  }

  public Storage build() throws IOException {
    File folder = ensureSubdir(rootDir, folderName);
    FolderManager folderManager = new FolderManager(folder, configuration, clock);
    return new Storage(folderManager);
  }

  private static File ensureSubdir(File rootDir, String child) throws IOException {
    File subdir = new File(rootDir, child);
    if (subdir.exists() || subdir.mkdirs()) {
      return subdir;
    }
    throw new IOException("Could not create the subdir: '" + child + "' inside: " + rootDir);
  }
}
