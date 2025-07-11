/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.opamp.client.internal.impl;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.opamp.client.internal.state.State;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("UnusedVariable")
class OpampClientStateTest {
  @Mock private State.RemoteConfigStatus remoteConfigStatus;
  @Mock private State.SequenceNum sequenceNum;
  @Mock private State.AgentDescription agentDescription;
  @Mock private State.Capabilities capabilities;
  @Mock private State.InstanceUid instanceUid;
  @Mock private State.Flags flags;
  @Mock private State.EffectiveConfig effectiveConfig;
  @InjectMocks private OpampClientState state;

  @Test
  void verifyAllFields() throws IllegalAccessException {
    List<State<?>> stateFields = new ArrayList<>();
    for (Field field : OpampClientState.class.getFields()) {
      if (State.class.isAssignableFrom(field.getType())) {
        stateFields.add((State<?>) field.get(state));
      }
    }

    assertThat(state.getAll()).containsAll(stateFields);
  }
}
