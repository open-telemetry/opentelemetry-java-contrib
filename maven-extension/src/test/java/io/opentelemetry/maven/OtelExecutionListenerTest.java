/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.maven;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class OtelExecutionListenerTest {

  @Test
  public void getPluginArtifactIdShortName_builtinPluginName() {
    OtelExecutionListener otelEventSpy = new OtelExecutionListener();
    String actual = otelEventSpy.getPluginArtifactIdShortName("maven-clean-plugin");
    String expected = "clean";
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void getPluginArtifactIdShortName_thirdPartyPluginName() {
    OtelExecutionListener otelEventSpy = new OtelExecutionListener();
    String actual = otelEventSpy.getPluginArtifactIdShortName("spotbugs-maven-plugin");
    String expected = "spotbugs";
    assertThat(actual).isEqualTo(expected);
  }
}
