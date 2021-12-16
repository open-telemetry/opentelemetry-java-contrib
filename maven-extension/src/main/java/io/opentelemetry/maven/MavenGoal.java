/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.maven;

import com.google.auto.value.AutoValue;
import org.apache.maven.plugin.MojoExecution;

@AutoValue
public abstract class MavenGoal {
  public static MavenGoal create(String groupId, String artifactId, String goal) {
    return new AutoValue_MavenGoal(groupId, artifactId, goal);
  }

  public static MavenGoal create(MojoExecution mojoExecution) {
    return create(
        mojoExecution.getGroupId(), mojoExecution.getArtifactId(), mojoExecution.getGoal());
  }

  abstract String groupId();

  abstract String artifactId();

  abstract String goal();

  @Override
  public final String toString() {
    return "MavenGoal{ "
        + groupId()
        + ":"
        + MavenUtils.getPluginArtifactIdShortName(artifactId())
        + ":"
        + goal()
        + "}";
  }
}
