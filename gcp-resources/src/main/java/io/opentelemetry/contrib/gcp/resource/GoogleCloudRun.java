/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.gcp.resource;

class GoogleCloudRun extends GoogleServerlessCompute {
  GoogleCloudRun(EnvironmentVariables environmentVariables, GcpMetadataConfig metadataConfig) {
    super(environmentVariables, metadataConfig);
  }

  @Override
  public GcpPlatformDetector.SupportedPlatform getSupportedPlatform() {
    return GcpPlatformDetector.SupportedPlatform.GOOGLE_CLOUD_RUN;
  }
}
