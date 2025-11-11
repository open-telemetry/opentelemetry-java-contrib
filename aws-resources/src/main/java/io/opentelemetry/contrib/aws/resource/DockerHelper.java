/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.aws.resource;

import static java.util.logging.Level.WARNING;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Logger;

class DockerHelper {

  private static final Logger logger = Logger.getLogger(DockerHelper.class.getName());
  private static final int CONTAINER_ID_LENGTH = 64;
  private static final String DEFAULT_CGROUP_PATH = "/proc/self/cgroup";

  private final String cgroupPath;

  DockerHelper() {
    this(DEFAULT_CGROUP_PATH);
  }

  // Visible for testing
  DockerHelper(String cgroupPath) {
    this.cgroupPath = cgroupPath;
  }

  /**
   * Get docker container id from local cgroup file.
   *
   * @return docker container ID. Empty string if it can`t be found.
   */
  public String getContainerId() {
    try (BufferedReader br = Files.newBufferedReader(Paths.get(cgroupPath))) {
      String line;
      while ((line = br.readLine()) != null) {
        if (line.length() > CONTAINER_ID_LENGTH) {
          return line.substring(line.length() - CONTAINER_ID_LENGTH);
        }
      }
    } catch (FileNotFoundException e) {
      logger.log(WARNING, "Failed to read container id, cgroup file does not exist.");
    } catch (IOException e) {
      logger.log(WARNING, "Unable to read container id: " + e.getMessage());
    }

    return "";
  }
}
