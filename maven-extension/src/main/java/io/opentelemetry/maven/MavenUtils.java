/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.maven;

import org.apache.maven.plugin.MojoExecution;

final class MavenUtils {
  private MavenUtils() {}

  /**
   * Shorten plugin identifiers.
   *
   * <p>Examples:
   *
   * <ul>
   *   <li>maven-clean-plugin -&gt; clean
   *   <li>sisu-maven-plugin -&gt; sisu
   *   <li>spotbugs-maven-plugin -&gt; spotbugs
   * </ul>
   *
   * @param pluginArtifactId the artifact ID of the mojo {@link MojoExecution#getArtifactId()}
   * @return shortened name
   */
  static String getPluginArtifactIdShortName(String pluginArtifactId) {
    if (pluginArtifactId.endsWith("-maven-plugin")) {
      return pluginArtifactId.substring(0, pluginArtifactId.length() - "-maven-plugin".length());
    } else if (pluginArtifactId.startsWith("maven-") && pluginArtifactId.endsWith("-plugin")) {
      return pluginArtifactId.substring(
          "maven-".length(), pluginArtifactId.length() - "-plugin".length());
    } else {
      return pluginArtifactId;
    }
  }
}
