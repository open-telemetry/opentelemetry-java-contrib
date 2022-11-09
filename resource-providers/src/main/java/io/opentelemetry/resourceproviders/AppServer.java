/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.resourceproviders;

import java.nio.file.Path;
import javax.annotation.Nullable;

/**
 * An interface that represents a single kind of application server and its specific configuration.
 */
interface AppServer {

  /** Path to directory to be scanned for deployments. */
  Path getDeploymentDir() throws Exception;

  /**
   * Returns a single class that, when present, determines that the given application server is
   * active/running.
   */
  Class<?> getServerClass();

  /**
   * Implementations for app servers that do not support ear files should override this method and
   * return false;
   */
  default boolean supportsEar() {
    return true;
  }

  /** Use to ignore default applications that are bundled with the app server. */
  default boolean isValidAppName(Path path) {
    return true;
  }

  /** Use to ignore default applications that are bundled with the app server. */
  default boolean isValidResult(Path path, @Nullable String result) {
    return true;
  }
}
