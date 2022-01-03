/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.maven.handler;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.maven.MavenGoal;
import io.opentelemetry.maven.OtelExecutionListener;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class MojoGoalExecutionHandlerConfigurationTest {

  @Test
  public void mojoGoalExecutionHandlers() {

    final Map<MavenGoal, MojoGoalExecutionHandler> actual =
        MojoGoalExecutionHandlerConfiguration.loadMojoGoalExecutionHandler(
            OtelExecutionListener.class.getClassLoader());
    assertThat(actual.size()).isEqualTo(5);
  }
}
