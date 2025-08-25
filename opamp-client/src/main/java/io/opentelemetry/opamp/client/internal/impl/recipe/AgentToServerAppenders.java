/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.opamp.client.internal.impl.recipe;

import io.opentelemetry.opamp.client.internal.impl.recipe.appenders.AgentDescriptionAppender;
import io.opentelemetry.opamp.client.internal.impl.recipe.appenders.AgentDisconnectAppender;
import io.opentelemetry.opamp.client.internal.impl.recipe.appenders.AgentToServerAppender;
import io.opentelemetry.opamp.client.internal.impl.recipe.appenders.CapabilitiesAppender;
import io.opentelemetry.opamp.client.internal.impl.recipe.appenders.EffectiveConfigAppender;
import io.opentelemetry.opamp.client.internal.impl.recipe.appenders.FlagsAppender;
import io.opentelemetry.opamp.client.internal.impl.recipe.appenders.InstanceUidAppender;
import io.opentelemetry.opamp.client.internal.impl.recipe.appenders.RemoteConfigStatusAppender;
import io.opentelemetry.opamp.client.internal.impl.recipe.appenders.SequenceNumberAppender;
import io.opentelemetry.opamp.client.internal.request.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * This class is internal and is hence not for public use. Its APIs are unstable and can change at
 * any time.
 */
public class AgentToServerAppenders {
  public final AgentDescriptionAppender agentDescriptionAppender;
  public final EffectiveConfigAppender effectiveConfigAppender;
  public final RemoteConfigStatusAppender remoteConfigStatusAppender;
  public final SequenceNumberAppender sequenceNumberAppender;
  public final CapabilitiesAppender capabilitiesAppender;
  public final InstanceUidAppender instanceUidAppender;
  public final FlagsAppender flagsAppender;
  public final AgentDisconnectAppender agentDisconnectAppender;
  private final Map<Field, AgentToServerAppender> allAppenders;

  public AgentToServerAppenders(
      AgentDescriptionAppender agentDescriptionAppender,
      EffectiveConfigAppender effectiveConfigAppender,
      RemoteConfigStatusAppender remoteConfigStatusAppender,
      SequenceNumberAppender sequenceNumberAppender,
      CapabilitiesAppender capabilitiesAppender,
      InstanceUidAppender instanceUidAppender,
      FlagsAppender flagsAppender,
      AgentDisconnectAppender agentDisconnectAppender) {
    this.agentDescriptionAppender = agentDescriptionAppender;
    this.effectiveConfigAppender = effectiveConfigAppender;
    this.remoteConfigStatusAppender = remoteConfigStatusAppender;
    this.sequenceNumberAppender = sequenceNumberAppender;
    this.capabilitiesAppender = capabilitiesAppender;
    this.instanceUidAppender = instanceUidAppender;
    this.flagsAppender = flagsAppender;
    this.agentDisconnectAppender = agentDisconnectAppender;

    Map<Field, AgentToServerAppender> appenders = new HashMap<>();
    appenders.put(Field.AGENT_DESCRIPTION, agentDescriptionAppender);
    appenders.put(Field.EFFECTIVE_CONFIG, effectiveConfigAppender);
    appenders.put(Field.REMOTE_CONFIG_STATUS, remoteConfigStatusAppender);
    appenders.put(Field.SEQUENCE_NUM, sequenceNumberAppender);
    appenders.put(Field.CAPABILITIES, capabilitiesAppender);
    appenders.put(Field.INSTANCE_UID, instanceUidAppender);
    appenders.put(Field.FLAGS, flagsAppender);
    appenders.put(Field.AGENT_DISCONNECT, agentDisconnectAppender);
    allAppenders = Collections.unmodifiableMap(appenders);
  }

  public AgentToServerAppender getForField(Field type) {
    if (!allAppenders.containsKey(type)) {
      throw new IllegalArgumentException("Field type " + type + " is not supported");
    }
    return allAppenders.get(type);
  }
}
