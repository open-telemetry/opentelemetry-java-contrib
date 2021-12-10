/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.maven;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.maven.handler.MojoGoalExecutionHandler;
import java.util.Collection;
import org.junit.jupiter.api.Test;

public class OtelExecutionListenerTest {

  @Test
  public void mojoGoalExecutionHandlers() {
    OtelExecutionListener otelExecutionListener = new OtelExecutionListener();

    Collection<MojoGoalExecutionHandler> actual =
        otelExecutionListener.mojoGoalExecutionHandlers.values();
    assertThat(actual.size()).isEqualTo(4);
  }
}
