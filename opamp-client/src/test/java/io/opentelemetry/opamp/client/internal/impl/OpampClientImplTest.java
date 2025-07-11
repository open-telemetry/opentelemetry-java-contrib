package io.opentelemetry.opamp.client.internal.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import io.opentelemetry.opamp.client.internal.OpampClient;
import io.opentelemetry.opamp.client.internal.request.Request;
import io.opentelemetry.opamp.client.internal.request.service.RequestService;
import io.opentelemetry.opamp.client.internal.response.MessageData;
import io.opentelemetry.opamp.client.internal.response.OpampServerResponseException;
import io.opentelemetry.opamp.client.internal.response.Response;
import io.opentelemetry.opamp.client.internal.state.State;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import okio.ByteString;
import opamp.proto.AgentCapabilities;
import opamp.proto.AgentConfigFile;
import opamp.proto.AgentConfigMap;
import opamp.proto.AgentDescription;
import opamp.proto.AgentIdentification;
import opamp.proto.AgentRemoteConfig;
import opamp.proto.AgentToServer;
import opamp.proto.AgentToServerFlags;
import opamp.proto.AnyValue;
import opamp.proto.EffectiveConfig;
import opamp.proto.KeyValue;
import opamp.proto.RemoteConfigStatus;
import opamp.proto.RemoteConfigStatuses;
import opamp.proto.ServerErrorResponse;
import opamp.proto.ServerToAgent;
import opamp.proto.ServerToAgentFlags;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OpampClientImplTest {
  @Mock private RequestService requestService;
  @Mock private OpampClient.Callbacks callbacks;
  private OpampClientState state;
  private OpampClientImpl client;
  private TestEffectiveConfig effectiveConfig;

  @BeforeEach
  void setUp() {
    effectiveConfig =
        new TestEffectiveConfig(
            new EffectiveConfig.Builder()
                .config_map(createAgentConfigMap("first", "first content"))
                .build());
    state =
        new OpampClientState(
            new State.RemoteConfigStatus(
                getRemoteConfigStatus(RemoteConfigStatuses.RemoteConfigStatuses_UNSET)),
            new State.SequenceNum(1L),
            new State.AgentDescription(new AgentDescription.Builder().build()),
            new State.Capabilities(
                (long) AgentCapabilities.AgentCapabilities_Unspecified.getValue()),
            new State.InstanceUid(new byte[] {1, 2, 3}),
            new State.Flags((long) AgentToServerFlags.AgentToServerFlags_Unspecified.getValue()),
            effectiveConfig);
    client = OpampClientImpl.create(requestService, state);
  }

  @Test
  void verifyStart() {
    client.start(callbacks);

    verify(requestService).start(client, client);

    // Check state observing
    clearInvocations(requestService);
    EffectiveConfig otherConfig =
        new EffectiveConfig.Builder()
            .config_map(createAgentConfigMap("other", "other value"))
            .build();
    effectiveConfig.config = otherConfig;
    effectiveConfig.notifyUpdate();

    verify(requestService).sendRequest();
    assertThat(client.get().getAgentToServer().effective_config).isEqualTo(otherConfig);
  }

  @Test
  void verifyStop() {
    client.start(callbacks);
    verify(requestService).start(client, client);

    client.stop();
    verify(requestService).stop();

    // Check state observing
    clearInvocations(requestService);
    effectiveConfig.notifyUpdate();

    verifyNoInteractions(requestService);
  }

  @Test
  void verifyStartOnlyOnce() {
    client.start(callbacks);

    try {
      client.start(callbacks);
      fail("Should have thrown an exception");
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("The client has already been started");
    }
  }

  @Test
  void checkRequestFields() {
    client.start(callbacks);

    AgentToServer firstRequest = client.get().getAgentToServer();

    assertThat(firstRequest.instance_uid.toByteArray()).isEqualTo(state.instanceUid.get());
    assertThat(firstRequest.sequence_num).isEqualTo(1);
    assertThat(firstRequest.capabilities).isEqualTo(state.capabilities.get());
    assertThat(firstRequest.agent_description).isNotNull();
    assertThat(firstRequest.effective_config).isNotNull();
    assertThat(firstRequest.remote_config_status).isNotNull();

    client.onRequestSuccess(null);

    // Second request
    AgentToServer secondRequest = client.get().getAgentToServer();

    assertThat(secondRequest.instance_uid.toByteArray()).isEqualTo(state.instanceUid.get());
    assertThat(secondRequest.sequence_num).isEqualTo(2);
    assertThat(secondRequest.capabilities).isEqualTo(state.capabilities.get());
    assertThat(secondRequest.agent_description).isNull();
    assertThat(secondRequest.effective_config).isNull();
    assertThat(secondRequest.remote_config_status).isNull();
  }

  @Test
  void verifyRequestBuildingAfterStopIsCalled() {
    client.start(callbacks);

    client.stop();

    Request request = client.get();
    assertThat(request.getAgentToServer().agent_disconnect).isNotNull();
  }

  @Test
  void onSuccess_withChangesToReport_notifyCallbackOnMessage() {
    AgentRemoteConfig remoteConfig =
        new AgentRemoteConfig.Builder()
            .config(createAgentConfigMap("someKey", "someValue"))
            .build();
    ServerToAgent serverToAgent = new ServerToAgent.Builder().remote_config(remoteConfig).build();
    client.start(callbacks);

    client.onRequestSuccess(Response.create(serverToAgent));

    verify(callbacks)
        .onMessage(client, MessageData.builder().setRemoteConfig(remoteConfig).build());
  }

  @Test
  void onSuccess_withNoChangesToReport_doNotNotifyCallbackOnMessage() {
    ServerToAgent serverToAgent = new ServerToAgent.Builder().build();
    client.start(callbacks);

    client.onRequestSuccess(Response.create(serverToAgent));

    verify(callbacks, never()).onMessage(any(), any());
  }

  @Test
  void verifyAgentDescriptionSetter() {
    AgentDescription agentDescription =
        getAgentDescriptionWithOneIdentifyingValue("service.name", "My service");

    // Update when changed
    client.setAgentDescription(agentDescription);
    verify(requestService).sendRequest();

    // Ignore when the provided value is the same as the current one
    clearInvocations(requestService);
    client.setAgentDescription(agentDescription);
    verify(requestService, never()).sendRequest();
  }

  @Test
  void verifyRemoteConfigStatusSetter() {
    // Update when changed
    client.setRemoteConfigStatus(
        getRemoteConfigStatus(RemoteConfigStatuses.RemoteConfigStatuses_APPLYING));
    verify(requestService).sendRequest();

    // Ignore when the provided value is the same as the current one
    clearInvocations(requestService);
    client.setRemoteConfigStatus(
        getRemoteConfigStatus(RemoteConfigStatuses.RemoteConfigStatuses_APPLYING));
    verify(requestService, never()).sendRequest();
  }

  @Test
  void onConnectionSuccessful_notifyCallback() {
    client.start(callbacks);

    client.onConnectionSuccess();

    verify(callbacks).onConnect(client);
    verify(callbacks, never()).onConnectFailed(any(), any());
  }

  @Test
  void onFailedResponse_keepFieldsForNextRequest() {
    // Adding a non-constant field
    AgentDescription agentDescription =
        getAgentDescriptionWithOneIdentifyingValue("service.namespace", "something");
    client.setAgentDescription(agentDescription);

    // Assert first request contains it
    assertThat(client.get().getAgentToServer().agent_description).isEqualTo(agentDescription);

    // On fail, keep agent description field
    client.onRequestFailed(new Throwable());
    assertThat(client.get().getAgentToServer().agent_description).isEqualTo(agentDescription);

    // When there's no failure, do not keep it.
    assertThat(client.get().getAgentToServer().agent_description).isNull();
  }

  @Test
  void onFailedResponse_withServerErrorData_notifyCallback() {
    client.start(callbacks);
    ServerErrorResponse errorResponse = new ServerErrorResponse.Builder().build();

    client.onRequestFailed(new OpampServerResponseException(errorResponse, "error message"));

    verify(callbacks).onErrorResponse(client, errorResponse);
    verify(callbacks, never()).onMessage(any(), any());
  }

  @Test
  void onConnectionFailed_notifyCallback() {
    client.start(callbacks);
    Throwable throwable = mock();

    client.onConnectionFailed(throwable);

    verify(callbacks).onConnectFailed(client, throwable);
    verify(callbacks, never()).onConnect(any());
  }

  @Test
  void verifyDisableCompressionWhenRequestedByServer() {
    ServerToAgent serverToAgent =
        new ServerToAgent.Builder()
            .flags(ServerToAgentFlags.ServerToAgentFlags_ReportFullState.getValue())
            .build();
    client.start(callbacks);

    // First payload contains compressable fields
    AgentToServer firstRequest = client.get().getAgentToServer();
    assertThat(firstRequest.agent_description).isNotNull();
    assertThat(firstRequest.effective_config).isNotNull();
    assertThat(firstRequest.remote_config_status).isNotNull();

    // Second payload doesn't contain compressable fields
    AgentToServer secondRequest = client.get().getAgentToServer();
    assertThat(secondRequest.agent_description).isNull();
    assertThat(secondRequest.effective_config).isNull();
    assertThat(secondRequest.remote_config_status).isNull();

    // When the server requests a full payload, send them again.
    client.onRequestSuccess(Response.create(serverToAgent));

    AgentToServer thirdRequest = client.get().getAgentToServer();
    assertThat(thirdRequest.agent_description).isNotNull();
    assertThat(thirdRequest.effective_config).isNotNull();
    assertThat(thirdRequest.remote_config_status).isNotNull();
  }

  @Test
  void verifySequenceNumberIncreasesOnCreatingRequest() {
    client.start(callbacks);
    assertThat(state.sequenceNum.get()).isEqualTo(1);

    client.get();

    assertThat(state.sequenceNum.get()).isEqualTo(2);
  }

  @Test
  void whenStatusIsUpdated_notifyServerImmediately() {
    client.setRemoteConfigStatus(
        getRemoteConfigStatus(RemoteConfigStatuses.RemoteConfigStatuses_UNSET));
    client.start(callbacks);
    clearInvocations(requestService);

    client.setRemoteConfigStatus(
        getRemoteConfigStatus(RemoteConfigStatuses.RemoteConfigStatuses_APPLYING));

    verify(requestService).sendRequest();
  }

  @Test
  void whenStatusIsNotUpdated_doNotNotifyServer() {
    client.setRemoteConfigStatus(
        getRemoteConfigStatus(RemoteConfigStatuses.RemoteConfigStatuses_APPLYING));
    client.start(callbacks);
    clearInvocations(requestService);

    client.setRemoteConfigStatus(
        getRemoteConfigStatus(RemoteConfigStatuses.RemoteConfigStatuses_APPLYING));

    verify(requestService, never()).sendRequest();
  }

  @Test
  void whenServerProvidesNewInstanceUid_useIt() {
    client.start(callbacks);
    byte[] serverProvidedUid = new byte[] {1, 2, 3};
    ServerToAgent response =
        new ServerToAgent.Builder()
            .agent_identification(
                new AgentIdentification.Builder()
                    .new_instance_uid(ByteString.of(serverProvidedUid))
                    .build())
            .build();

    client.onRequestSuccess(Response.create(response));

    assertThat(state.instanceUid.get()).isEqualTo(serverProvidedUid);
  }

  private static RemoteConfigStatus getRemoteConfigStatus(RemoteConfigStatuses status) {
    return new RemoteConfigStatus.Builder().status(status).build();
  }

  private static AgentConfigMap createAgentConfigMap(String key, String content) {
    Map<String, AgentConfigFile> keyToFile = new HashMap<>();
    keyToFile.put(key, new AgentConfigFile.Builder().body(ByteString.encodeUtf8(content)).build());
    return new AgentConfigMap.Builder().config_map(keyToFile).build();
  }

  private static AgentDescription getAgentDescriptionWithOneIdentifyingValue(
      String key, String value) {
    KeyValue keyValue =
        new KeyValue.Builder()
            .key(key)
            .value(new AnyValue.Builder().string_value(value).build())
            .build();
    List<KeyValue> keyValues = new ArrayList<>();
    keyValues.add(keyValue);
    return new AgentDescription.Builder().identifying_attributes(keyValues).build();
  }

  private static class TestEffectiveConfig extends State.EffectiveConfig {
    private opamp.proto.EffectiveConfig config;

    public TestEffectiveConfig(opamp.proto.EffectiveConfig initialValue) {
      config = initialValue;
    }

    @Override
    public opamp.proto.EffectiveConfig get() {
      return config;
    }
  }
}
