/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.maven;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MavenUtilsTests {

  @Test
  public void getPluginArtifactIdShortName_builtinPluginName() {
    String actual = MavenUtils.getPluginArtifactIdShortName("maven-clean-plugin");
    String expected = "clean";
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void getPluginArtifactIdShortName_thirdPartyPluginName() {
    String actual = MavenUtils.getPluginArtifactIdShortName("spotbugs-maven-plugin");
    String expected = "spotbugs";
    assertThat(actual).isEqualTo(expected);
  }
}
