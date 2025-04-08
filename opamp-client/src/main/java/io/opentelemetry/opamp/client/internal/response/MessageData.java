/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.opamp.client.internal.response;

import com.google.auto.value.AutoValue;
import io.opentelemetry.opamp.client.internal.OpampClient;
import javax.annotation.Nullable;
import opamp.proto.AgentRemoteConfig;

/**
 * Data class provided in {@link OpampClient.Callbacks#onMessage(OpampClient, MessageData)} with
 * Server's provided status changes.
 */
@AutoValue
public abstract class MessageData {
  @Nullable
  public abstract AgentRemoteConfig getRemoteConfig();

  public static Builder builder() {
    return new AutoValue_MessageData.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setRemoteConfig(AgentRemoteConfig remoteConfig);

    public abstract MessageData build();
  }
}
