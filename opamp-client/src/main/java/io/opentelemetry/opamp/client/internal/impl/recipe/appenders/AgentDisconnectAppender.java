/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.opamp.client.internal.impl.recipe.appenders;

import opamp.proto.AgentDisconnect;
import opamp.proto.AgentToServer;

/**
 * This class is internal and is hence not for public use. Its APIs are unstable and can change at
 * any time.
 */
public class AgentDisconnectAppender implements AgentToServerAppender {

  public static AgentDisconnectAppender create() {
    return new AgentDisconnectAppender();
  }

  private AgentDisconnectAppender() {}

  @Override
  public void appendTo(AgentToServer.Builder builder) {
    builder.agent_disconnect(new AgentDisconnect.Builder().build());
  }
}
