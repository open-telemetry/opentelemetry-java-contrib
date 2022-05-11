/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.staticinstrumenter.agent.main;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** A holder for additional classes created by the agent. */
public final class AdditionalClasses {

  private static final Map<String, byte[]> additionalClasses = new ConcurrentHashMap<>();

  public static Map<String, byte[]> get() {
    return additionalClasses;
  }

  public static void put(String name, byte[] code) {
    additionalClasses.put(name, code);
  }

  private AdditionalClasses() {}
}
