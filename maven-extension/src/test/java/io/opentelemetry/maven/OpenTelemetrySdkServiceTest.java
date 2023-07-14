/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.maven;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class OpenTelemetrySdkServiceTest {

  /** Verify default `service.name` */
  @Test
  @Disabled
  public void testDefaultConfiguration() {
    testConfiguration("maven");
  }

  /** Verify overwritten `service.name` */
  @Test
  @Disabled
  public void testOverwrittenConfiguration() {
    System.setProperty("otel.service.name", "my-maven");
    try {
      testConfiguration("my-maven");
    } finally {
      System.clearProperty("otel.service.name");
    }
  }

  void testConfiguration(String expectedServiceName) {
//    OpenTelemetrySdkService openTelemetrySdkService = new OpenTelemetrySdkService();
//    openTelemetrySdkService.initialize();
//    try {
//      Resource resource = openTelemetrySdkService.autoConfiguredOpenTelemetrySdk.getResource();
//      assertThat(resource.getAttribute(ResourceAttributes.SERVICE_NAME))
//          .isEqualTo(expectedServiceName);
//    } finally {
//      openTelemetrySdkService.dispose();
//      GlobalOpenTelemetry.resetForTest();
//      GlobalEventEmitterProvider.resetForTest();
//    }
  }
}
