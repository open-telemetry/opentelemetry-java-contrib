/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.gcp.resource;

class GoogleCloudFunction extends GoogleServerlessCompute {
  GoogleCloudFunction(EnvironmentVariables environmentVariables, GcpMetadataConfig metadataConfig) {
    super(environmentVariables, metadataConfig);
  }

  @Override
  public GcpPlatformDetector.SupportedPlatform getSupportedPlatform() {
    return GcpPlatformDetector.SupportedPlatform.GOOGLE_CLOUD_FUNCTIONS;
  }
}
