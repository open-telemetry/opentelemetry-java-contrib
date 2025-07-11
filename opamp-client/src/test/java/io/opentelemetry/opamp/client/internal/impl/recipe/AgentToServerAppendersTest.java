package io.opentelemetry.opamp.client.internal.impl.recipe;

import static org.assertj.core.api.Assertions.assertThat;

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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AgentToServerAppendersTest {
  @Mock private AgentDescriptionAppender agentDescriptionAppender;
  @Mock private EffectiveConfigAppender effectiveConfigAppender;
  @Mock private RemoteConfigStatusAppender remoteConfigStatusAppender;
  @Mock private SequenceNumberAppender sequenceNumberAppender;
  @Mock private CapabilitiesAppender capabilitiesAppender;
  @Mock private FlagsAppender flagsAppender;
  @Mock private InstanceUidAppender instanceUidAppender;
  @Mock private AgentDisconnectAppender agentDisconnectAppender;
  @InjectMocks private AgentToServerAppenders appenders;

  @Test
  void verifyAppenderList() {
    verifyMapping(Field.AGENT_DESCRIPTION, agentDescriptionAppender);
    verifyMapping(Field.EFFECTIVE_CONFIG, effectiveConfigAppender);
    verifyMapping(Field.REMOTE_CONFIG_STATUS, remoteConfigStatusAppender);
    verifyMapping(Field.SEQUENCE_NUM, sequenceNumberAppender);
    verifyMapping(Field.CAPABILITIES, capabilitiesAppender);
    verifyMapping(Field.INSTANCE_UID, instanceUidAppender);
    verifyMapping(Field.FLAGS, flagsAppender);
    verifyMapping(Field.AGENT_DISCONNECT, agentDisconnectAppender);
  }

  private void verifyMapping(Field type, AgentToServerAppender appender) {
    assertThat(appenders.getForField(type)).isEqualTo(appender);
  }
}
