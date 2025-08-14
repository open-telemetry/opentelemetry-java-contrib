/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.opamp.client.internal.impl.recipe.appenders;

import java.util.function.Supplier;
import opamp.proto.AgentDescription;
import opamp.proto.AgentToServer;

/**
 * This class is internal and is hence not for public use. Its APIs are unstable and can change at
 * any time.
 */
public final class AgentDescriptionAppender implements AgentToServerAppender {
  private final Supplier<AgentDescription> data;

  public static AgentDescriptionAppender create(Supplier<AgentDescription> data) {
    return new AgentDescriptionAppender(data);
  }

  private AgentDescriptionAppender(Supplier<AgentDescription> data) {
    this.data = data;
  }

  @Override
  public void appendTo(AgentToServer.Builder builder) {
    builder.agent_description(data.get());
  }
}
