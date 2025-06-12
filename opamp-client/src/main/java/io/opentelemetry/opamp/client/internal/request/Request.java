/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.opamp.client.internal.request;

import com.google.auto.value.AutoValue;
import opamp.proto.AgentToServer;

/** Wrapper class for "AgentToServer" request body. */
@AutoValue
public abstract class Request {
  public abstract AgentToServer getAgentToServer();

  public static Request create(AgentToServer agentToServer) {
    return new AutoValue_Request(agentToServer);
  }
}
