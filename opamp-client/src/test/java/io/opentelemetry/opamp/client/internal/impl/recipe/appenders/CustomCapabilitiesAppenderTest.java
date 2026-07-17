/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.opamp.client.internal.impl.recipe.appenders;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;
import opamp.proto.AgentToServer;
import opamp.proto.CustomCapabilities;
import org.junit.jupiter.api.Test;

class CustomCapabilitiesAppenderTest {

  @Test
  void shouldAppendProvidedCustomCapabilities() {
    CustomCapabilities customCapabilities =
        new CustomCapabilities.Builder()
            .capabilities(Collections.singletonList("com.example.pause"))
            .build();
    CustomCapabilitiesAppender appender =
        CustomCapabilitiesAppender.create(() -> customCapabilities);

    AgentToServer.Builder builder = new AgentToServer.Builder();
    appender.appendTo(builder);

    assertThat(builder.build().custom_capabilities).isEqualTo(customCapabilities);
  }

  @Test
  void shouldNotAppendCustomCapabilitiesIfNotProvided() {
    CustomCapabilitiesAppender appender =
        CustomCapabilitiesAppender.create(new AtomicReference<CustomCapabilities>()::get);

    AgentToServer.Builder builder = new AgentToServer.Builder();
    appender.appendTo(builder);

    assertThat(builder.build().custom_capabilities).isNull();
  }
}
