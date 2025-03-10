/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.storage;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.opentelemetry.contrib.disk.buffering.config.StorageConfiguration;
import io.opentelemetry.sdk.common.Clock;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class StorageBuilder {

  private static final Logger logger = Logger.getLogger(StorageBuilder.class.getName());

  private String folderName = "data";
  private StorageConfiguration configuration = StorageConfiguration.getDefault(new File("."));
  private Clock clock = Clock.getDefault();

  StorageBuilder() {}

  @CanIgnoreReturnValue
  public StorageBuilder setFolderName(String folderName) {
    this.folderName = folderName;
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
    File folder = ensureSubdir(configuration.getRootDir(), folderName);
    FolderManager folderManager = new FolderManager(folder, configuration, clock);
    if (configuration.isDebugEnabled()) {
      logger.log(Level.INFO, "Building storage with configuration => " + configuration);
    }
    return new Storage(folderManager, configuration.isDebugEnabled());
  }

  private static File ensureSubdir(File rootDir, String child) throws IOException {
    File subdir = new File(rootDir, child);
    if (subdir.exists() || subdir.mkdirs()) {
      return subdir;
    }
    throw new IOException("Could not create the subdir: '" + child + "' inside: " + rootDir);
  }
}
