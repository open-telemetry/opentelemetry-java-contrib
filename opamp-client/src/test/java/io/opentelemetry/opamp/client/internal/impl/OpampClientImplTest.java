/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.opamp.client.internal.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import io.opentelemetry.opamp.client.internal.OpampClient;
import io.opentelemetry.opamp.client.internal.connectivity.http.OkHttpSender;
import io.opentelemetry.opamp.client.internal.request.Request;
import io.opentelemetry.opamp.client.internal.request.service.HttpRequestService;
import io.opentelemetry.opamp.client.internal.request.service.RequestService;
import io.opentelemetry.opamp.client.internal.response.MessageData;
import io.opentelemetry.opamp.client.internal.state.State;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import javax.annotation.Nonnull;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import mockwebserver3.RecordedRequest;
import mockwebserver3.junit5.StartStop;
import okio.Buffer;
import okio.ByteString;
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
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OpampClientImplTest {
  private RequestService requestService;
  private OpampClientState state;
  private OpampClientImpl client;
  private TestEffectiveConfig effectiveConfig;
  private TestCallbacks callbacks;
  @StartStop private final MockWebServer server = new MockWebServer();

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
            new State.Capabilities(5L),
            new State.InstanceUid(new byte[] {1, 2, 3}),
            new State.Flags((long) AgentToServerFlags.AgentToServerFlags_Unspecified.getValue()),
            effectiveConfig);
    requestService = createHttpService();
  }

  @AfterEach
  void tearDown() {
    client.stop();
  }

  @Test
  void verifyFieldsSent() {
    // Check first request
    ServerToAgent response = new ServerToAgent.Builder().build();
    RecordedRequest firstRequest = initializeClient(response);
    AgentToServer firstMessage = getAgentToServerMessage(firstRequest);

    // Required first request fields
    assertThat(firstMessage.instance_uid).isNotNull();
    assertThat(firstMessage.sequence_num).isEqualTo(1);
    assertThat(firstMessage.capabilities).isEqualTo(state.capabilities.get());
    assertThat(firstMessage.agent_description).isEqualTo(state.agentDescription.get());
    assertThat(firstMessage.effective_config).isEqualTo(state.effectiveConfig.get());
    assertThat(firstMessage.remote_config_status).isEqualTo(state.remoteConfigStatus.get());

    // Check second request
    enqueueServerToAgentResponse(response);
    RemoteConfigStatus remoteConfigStatus =
        new RemoteConfigStatus.Builder()
            .status(RemoteConfigStatuses.RemoteConfigStatuses_APPLYING)
            .build();
    client.setRemoteConfigStatus(remoteConfigStatus);

    RecordedRequest secondRequest = takeRequest();
    AgentToServer secondMessage = getAgentToServerMessage(secondRequest);

    // Verify only changed and required fields are present
    assertThat(secondMessage.instance_uid).isNotNull();
    assertThat(secondMessage.sequence_num).isEqualTo(2);
    assertThat(firstMessage.capabilities).isEqualTo(state.capabilities.get());
    assertThat(secondMessage.agent_description).isNull();
    assertThat(secondMessage.effective_config).isNull();
    assertThat(secondMessage.remote_config_status).isEqualTo(remoteConfigStatus);

    // Check state observing
    enqueueServerToAgentResponse(response);
    EffectiveConfig otherConfig =
        new EffectiveConfig.Builder()
            .config_map(createAgentConfigMap("other", "other value"))
            .build();
    effectiveConfig.config = otherConfig;
    effectiveConfig.notifyUpdate();

    // Check third request
    RecordedRequest thirdRequest = takeRequest();
    AgentToServer thirdMessage = getAgentToServerMessage(thirdRequest);

    assertThat(thirdMessage.instance_uid).isNotNull();
    assertThat(thirdMessage.sequence_num).isEqualTo(3);
    assertThat(firstMessage.capabilities).isEqualTo(state.capabilities.get());
    assertThat(thirdMessage.agent_description).isNull();
    assertThat(thirdMessage.remote_config_status).isNull();
    assertThat(thirdMessage.effective_config)
        .isEqualTo(otherConfig); // it was changed via observable state

    // Check when the server requests for all fields

    ServerToAgent reportFullState =
        new ServerToAgent.Builder()
            .flags(ServerToAgentFlags.ServerToAgentFlags_ReportFullState.getValue())
            .build();
    enqueueServerToAgentResponse(reportFullState);
    requestService.sendRequest();
    takeRequest(); // Notifying the client to send all fields next time

    // Request with all fields
    enqueueServerToAgentResponse(new ServerToAgent.Builder().build());
    requestService.sendRequest();

    AgentToServer fullRequestedMessage = getAgentToServerMessage(takeRequest());

    // Required first request fields
    assertThat(fullRequestedMessage.instance_uid).isNotNull();
    assertThat(fullRequestedMessage.sequence_num).isEqualTo(5);
    assertThat(fullRequestedMessage.capabilities).isEqualTo(state.capabilities.get());
    assertThat(fullRequestedMessage.agent_description).isEqualTo(state.agentDescription.get());
    assertThat(fullRequestedMessage.effective_config).isEqualTo(state.effectiveConfig.get());
    assertThat(fullRequestedMessage.remote_config_status).isEqualTo(state.remoteConfigStatus.get());
  }

  @Test
  void verifyStop() {
    initializeClient();

    enqueueServerToAgentResponse(new ServerToAgent.Builder().build());
    client.stop();

    AgentToServer agentToServerMessage = getAgentToServerMessage(takeRequest());
    assertThat(agentToServerMessage.agent_disconnect).isNotNull();
  }

  @Test
  void verifyStartOnlyOnce() {
    initializeClient();
    try {
      client.start(callbacks);
      fail("Should have thrown an exception");
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("The client has already been started");
    }
  }

  @Test
  void onSuccess_withChangesToReport_notifyCallbackOnMessage() {
    initializeClient();
    AgentRemoteConfig remoteConfig =
        new AgentRemoteConfig.Builder()
            .config(createAgentConfigMap("someKey", "someValue"))
            .build();
    ServerToAgent serverToAgent = new ServerToAgent.Builder().remote_config(remoteConfig).build();
    enqueueServerToAgentResponse(serverToAgent);

    // Force request
    requestService.sendRequest();

    // Await for onMessage call
    await().atMost(Duration.ofSeconds(1)).until(() -> callbacks.onMessageCalls.get() == 1);

    verify(callbacks).onMessage(MessageData.builder().setRemoteConfig(remoteConfig).build());
  }

  @Test
  void onSuccess_withNoChangesToReport_doNotNotifyCallbackOnMessage() {
    initializeClient();
    ServerToAgent serverToAgent = new ServerToAgent.Builder().build();
    enqueueServerToAgentResponse(serverToAgent);

    // Force request
    requestService.sendRequest();

    // Giving some time for the callback to get called
    await().during(Duration.ofSeconds(1));

    verify(callbacks, never()).onMessage(any());
  }

  @Test
  void verifyAgentDescriptionSetter() {
    initializeClient();
    AgentDescription agentDescription =
        getAgentDescriptionWithOneIdentifyingValue("service.name", "My service");

    // Update when changed
    enqueueServerToAgentResponse(new ServerToAgent.Builder().build());
    client.setAgentDescription(agentDescription);
    assertThat(takeRequest()).isNotNull();

    // Ignore when the provided value is the same as the current one
    enqueueServerToAgentResponse(new ServerToAgent.Builder().build());
    client.setAgentDescription(agentDescription);
    assertThat(takeRequest()).isNull();
  }

  @Test
  void verifyRemoteConfigStatusSetter() {
    initializeClient();
    RemoteConfigStatus remoteConfigStatus =
        getRemoteConfigStatus(RemoteConfigStatuses.RemoteConfigStatuses_APPLYING);

    // Update when changed
    enqueueServerToAgentResponse(new ServerToAgent.Builder().build());
    client.setRemoteConfigStatus(remoteConfigStatus);
    assertThat(takeRequest()).isNotNull();

    // Ignore when the provided value is the same as the current one
    enqueueServerToAgentResponse(new ServerToAgent.Builder().build());
    client.setRemoteConfigStatus(remoteConfigStatus);
    assertThat(takeRequest()).isNull();
  }

  @Test
  void onConnectionSuccessful_notifyCallback() {
    initializeClient();

    await().atMost(Duration.ofSeconds(1)).until(() -> callbacks.onConnectCalls.get() == 1);

    verify(callbacks).onConnect();
    verify(callbacks, never()).onConnectFailed(any());
  }

  @Test
  void onFailedResponse_keepFieldsForNextRequest() {
    initializeClient();

    // Mock failed request
    server.enqueue(new MockResponse.Builder().code(404).build());

    // Adding a non-constant field
    AgentDescription agentDescription =
        getAgentDescriptionWithOneIdentifyingValue("service.namespace", "something");
    client.setAgentDescription(agentDescription);

    // Assert first request contains it
    assertThat(getAgentToServerMessage(takeRequest()).agent_description)
        .isEqualTo(agentDescription);

    // Since it failed, send the agent description field in the next request
    enqueueServerToAgentResponse(new ServerToAgent.Builder().build());
    requestService.sendRequest();
    assertThat(getAgentToServerMessage(takeRequest()).agent_description)
        .isEqualTo(agentDescription);

    // When there's no failure, do not keep it.
    enqueueServerToAgentResponse(new ServerToAgent.Builder().build());
    requestService.sendRequest();
    assertThat(getAgentToServerMessage(takeRequest()).agent_description).isNull();
  }

  @Test
  void onFailedResponse_withServerErrorData_notifyCallback() {
    initializeClient();

    ServerErrorResponse errorResponse = new ServerErrorResponse.Builder().build();
    enqueueServerToAgentResponse(new ServerToAgent.Builder().error_response(errorResponse).build());

    // Force request
    requestService.sendRequest();

    await().atMost(Duration.ofSeconds(1)).until(() -> callbacks.onErrorResponseCalls.get() == 1);

    verify(callbacks).onErrorResponse(errorResponse);
    verify(callbacks, never()).onMessage(any());
  }

  @Test
  void onConnectionFailed_notifyCallback() {
    initializeClient();
    Throwable throwable = new Throwable();

    client.onConnectionFailed(throwable);

    verify(callbacks).onConnectFailed(throwable);
  }

  @Test
  void whenServerProvidesNewInstanceUid_useIt() {
    initializeClient();
    byte[] initialUid = state.instanceUid.get();

    byte[] serverProvidedUid = new byte[] {1, 2, 3};
    ServerToAgent response =
        new ServerToAgent.Builder()
            .agent_identification(
                new AgentIdentification.Builder()
                    .new_instance_uid(ByteString.of(serverProvidedUid))
                    .build())
            .build();

    enqueueServerToAgentResponse(response);
    requestService.sendRequest();

    await().atMost(Duration.ofSeconds(1)).until(() -> state.instanceUid.get() != initialUid);

    assertThat(state.instanceUid.get()).isEqualTo(serverProvidedUid);
  }

  private static AgentToServer getAgentToServerMessage(RecordedRequest request) {
    try {
      return AgentToServer.ADAPTER.decode(Objects.requireNonNull(request.getBody()));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private RecordedRequest takeRequest() {
    try {
      return server.takeRequest(1, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  private void enqueueServerToAgentResponse(ServerToAgent response) {
    server.enqueue(getMockResponse(response));
  }

  @Nonnull
  private static MockResponse getMockResponse(ServerToAgent response) {
    Buffer bodyBuffer = new Buffer();
    bodyBuffer.write(response.encode());
    return new MockResponse.Builder().code(200).body(bodyBuffer).build();
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

  private RecordedRequest initializeClient() {
    return initializeClient(new ServerToAgent.Builder().build());
  }

  private RecordedRequest initializeClient(ServerToAgent initialResponse) {
    client = OpampClientImpl.create(requestService, state);

    // Prepare first request on start
    enqueueServerToAgentResponse(initialResponse);

    callbacks = spy(new TestCallbacks());
    client.start(callbacks);
    return takeRequest();
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

  private RequestService createHttpService() {
    return new TestHttpRequestService(
        HttpRequestService.create(OkHttpSender.create(server.url("/v1/opamp").toString())));
  }

  private static class TestHttpRequestService implements RequestService {
    private final HttpRequestService delegate;

    private TestHttpRequestService(HttpRequestService delegate) {
      this.delegate = delegate;
    }

    @Override
    public void start(Callback callback, Supplier<Request> requestSupplier) {
      delegate.start(callback, requestSupplier);
    }

    @Override
    public void sendRequest() {
      delegate.sendRequest();
    }

    @Override
    public void stop() {
      // This is to verify agent disconnect field presence for the websocket use case.
      delegate.sendRequest();
      delegate.stop();
    }
  }

  private static class TestCallbacks implements OpampClient.Callbacks {
    private final AtomicInteger onConnectCalls = new AtomicInteger();
    private final AtomicInteger onConnectFailedCalls = new AtomicInteger();
    private final AtomicInteger onErrorResponseCalls = new AtomicInteger();
    private final AtomicInteger onMessageCalls = new AtomicInteger();

    @Override
    public void onConnect() {
      onConnectCalls.incrementAndGet();
    }

    @Override
    public void onConnectFailed(@Nullable Throwable throwable) {
      onConnectFailedCalls.incrementAndGet();
    }

    @Override
    public void onErrorResponse(ServerErrorResponse errorResponse) {
      onErrorResponseCalls.incrementAndGet();
    }

    @Override
    public void onMessage(MessageData messageData) {
      onMessageCalls.incrementAndGet();
    }
  }
}
