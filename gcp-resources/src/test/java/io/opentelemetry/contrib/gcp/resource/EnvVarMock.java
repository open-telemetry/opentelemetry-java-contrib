/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.gcp.resource;

import java.util.Map;

class EnvVarMock implements EnvironmentVariables {
  private final Map<String, String> mock;

  public EnvVarMock(Map<String, String> mock) {
    this.mock = mock;
  }

  @Override
  public String get(String key) {
    return mock.get(key);
  }
}
