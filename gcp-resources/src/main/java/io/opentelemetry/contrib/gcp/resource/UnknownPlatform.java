/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.gcp.resource;

import java.util.Collections;
import java.util.Map;

class UnknownPlatform implements DetectedPlatform {

  UnknownPlatform() {}

  @Override
  public GcpPlatformDetector.SupportedPlatform getSupportedPlatform() {
    return GcpPlatformDetector.SupportedPlatform.UNKNOWN_PLATFORM;
  }

  @Override
  public String getProjectId() {
    return "";
  }

  @Override
  public Map<String, String> getAttributes() {
    return Collections.emptyMap();
  }
}
