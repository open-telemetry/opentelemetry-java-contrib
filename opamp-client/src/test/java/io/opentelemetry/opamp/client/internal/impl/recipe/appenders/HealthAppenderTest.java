/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.opamp.client.internal.impl.recipe.appenders;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicReference;
import opamp.proto.AgentToServer;
import opamp.proto.ComponentHealth;
import org.junit.jupiter.api.Test;

class HealthAppenderTest {

  @Test
  void shouldAppendProvidedHealth() {
    ComponentHealth health =
        new ComponentHealth.Builder()
            .healthy(true)
            .start_time_unix_nano(123L)
            .status("running")
            .status_time_unix_nano(456L)
            .build();
    HealthAppender appender = HealthAppender.create(() -> health);

    AgentToServer.Builder builder = new AgentToServer.Builder();
    appender.appendTo(builder);

    assertThat(builder.build().health).isEqualTo(health);
  }

  @Test
  void shouldNotAppendHealthIfNotProvided() {
    HealthAppender appender = HealthAppender.create(new AtomicReference<ComponentHealth>()::get);

    AgentToServer.Builder builder = new AgentToServer.Builder();
    appender.appendTo(builder);

    assertThat(builder.build().health).isNull();
  }
}
