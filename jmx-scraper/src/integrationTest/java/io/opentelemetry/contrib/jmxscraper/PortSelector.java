/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxscraper;

import java.io.IOException;
import java.net.Socket;
import java.util.Random;

/** Class used for finding random free network port from range 1024-65535 */
public class PortSelector {
  private static final Random random = new Random(System.currentTimeMillis());

  private static final int MIN_PORT = 1024;
  private static final int MAX_PORT = 65535;

  private PortSelector() {}

  /**
   * @return random available TCP port
   */
  public static synchronized int getAvailableRandomPort() {
    int port;

    do {
      port = random.nextInt(MAX_PORT - MIN_PORT + 1) + MIN_PORT;
    } while (!isPortAvailable(port));

    return port;
  }

  private static boolean isPortAvailable(int port) {
    // see https://stackoverflow.com/a/13826145 for the chosen implementation
    try (Socket s = new Socket("localhost", port)) {
      return false;
    } catch (IOException e) {
      return true;
    }
  }
}
