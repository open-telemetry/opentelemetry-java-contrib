/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.opamp.client.internal.impl;

import io.opentelemetry.opamp.client.internal.state.State;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This class is internal and is hence not for public use. Its APIs are unstable and can change at
 * any time.
 */
public final class OpampClientState {
  public final State.RemoteConfigStatus remoteConfigStatus;
  public final State.SequenceNum sequenceNum;
  public final State.AgentDescription agentDescription;
  public final State.Capabilities capabilities;
  public final State.InstanceUid instanceUid;
  public final State.Flags flags;
  public final State.EffectiveConfig effectiveConfig;
  private final List<State<?>> items;

  public OpampClientState(
      State.RemoteConfigStatus remoteConfigStatus,
      State.SequenceNum sequenceNum,
      State.AgentDescription agentDescription,
      State.Capabilities capabilities,
      State.InstanceUid instanceUid,
      State.Flags flags,
      State.EffectiveConfig effectiveConfig) {
    this.remoteConfigStatus = remoteConfigStatus;
    this.sequenceNum = sequenceNum;
    this.agentDescription = agentDescription;
    this.capabilities = capabilities;
    this.instanceUid = instanceUid;
    this.flags = flags;
    this.effectiveConfig = effectiveConfig;

    List<State<?>> providedItems = new ArrayList<>();
    providedItems.add(remoteConfigStatus);
    providedItems.add(sequenceNum);
    providedItems.add(agentDescription);
    providedItems.add(capabilities);
    providedItems.add(instanceUid);
    providedItems.add(flags);
    providedItems.add(effectiveConfig);

    items = Collections.unmodifiableList(providedItems);
  }

  public List<State<?>> getAll() {
    return items;
  }
}
