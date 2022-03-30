/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.staticinstrumenter.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Creates and removes temporary directories. */
public final class TmpDirManager {

  private static final Logger logger = LoggerFactory.getLogger(TmpDirManager.class);
  private final Path tmpDir;

  public TmpDirManager(String rootDirPrefix) throws IOException {
    this.tmpDir = Files.createTempDirectory(rootDirPrefix);
    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  try {
                    Files.delete(tmpDir);
                  } catch (IOException e) {
                    logger.debug("Could delete the temporary directory.");
                  }
                }));
  }

  public Path createDir(String path) throws IOException {
    return Files.createDirectories(tmpDir.resolve(path));
  }

  public Path getTmpFile(String dir, String prefix, String suffix) throws IOException {
    return Files.createTempFile(createDir(dir), prefix, suffix);
  }
}
