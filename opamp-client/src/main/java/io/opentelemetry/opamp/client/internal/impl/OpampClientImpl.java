/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.opamp.client.internal.impl;

import io.opentelemetry.opamp.client.internal.OpampClient;
import io.opentelemetry.opamp.client.internal.impl.recipe.AgentToServerAppenders;
import io.opentelemetry.opamp.client.internal.impl.recipe.RecipeManager;
import io.opentelemetry.opamp.client.internal.impl.recipe.RequestRecipe;
import io.opentelemetry.opamp.client.internal.impl.recipe.appenders.AgentDescriptionAppender;
import io.opentelemetry.opamp.client.internal.impl.recipe.appenders.AgentDisconnectAppender;
import io.opentelemetry.opamp.client.internal.impl.recipe.appenders.CapabilitiesAppender;
import io.opentelemetry.opamp.client.internal.impl.recipe.appenders.EffectiveConfigAppender;
import io.opentelemetry.opamp.client.internal.impl.recipe.appenders.FlagsAppender;
import io.opentelemetry.opamp.client.internal.impl.recipe.appenders.InstanceUidAppender;
import io.opentelemetry.opamp.client.internal.impl.recipe.appenders.RemoteConfigStatusAppender;
import io.opentelemetry.opamp.client.internal.impl.recipe.appenders.SequenceNumberAppender;
import io.opentelemetry.opamp.client.internal.request.Field;
import io.opentelemetry.opamp.client.internal.request.Request;
import io.opentelemetry.opamp.client.internal.request.service.RequestService;
import io.opentelemetry.opamp.client.internal.response.MessageData;
import io.opentelemetry.opamp.client.internal.response.OpampServerResponseException;
import io.opentelemetry.opamp.client.internal.response.Response;
import io.opentelemetry.opamp.client.internal.state.ObservableState;
import io.opentelemetry.opamp.client.internal.state.State;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import okio.ByteString;
import opamp.proto.AgentDescription;
import opamp.proto.AgentToServer;
import opamp.proto.RemoteConfigStatus;
import opamp.proto.ServerErrorResponse;
import opamp.proto.ServerToAgent;
import opamp.proto.ServerToAgentFlags;

/**
 * This class is internal and is hence not for public use. Its APIs are unstable and can change at
 * any time.
 */
public final class OpampClientImpl
    implements OpampClient, ObservableState.Listener, RequestService.Callback, Supplier<Request> {
  private final RequestService requestService;
  private final AgentToServerAppenders appenders;
  private final OpampClientState state;
  private final RecipeManager recipeManager;
  private final AtomicBoolean isRunning = new AtomicBoolean(false);
  private final AtomicBoolean hasStopped = new AtomicBoolean(false);
  @Nullable private Callbacks callbacks;

  /** Fields that must always be sent. */
  private static final List<Field> CONSTANT_FIELDS;

  /**
   * Fields that should only be sent in the first message and then omitted in following messages,
   * unless their value changes or the server requests a full message.
   *
   * <p>Refer to <a
   * href="https://github.com/open-telemetry/opamp-spec/blob/main/specification.md#agent-status-compression">the
   * docs</a> for more details.
   */
  private static final List<Field> COMPRESSABLE_FIELDS;

  static {
    // Constant fields init
    List<Field> constantFields = new ArrayList<>();
    constantFields.add(Field.INSTANCE_UID);
    constantFields.add(Field.SEQUENCE_NUM);
    constantFields.add(Field.CAPABILITIES);
    CONSTANT_FIELDS = Collections.unmodifiableList(constantFields);

    // Compressable fields init
    List<Field> compressableFields = new ArrayList<>();
    compressableFields.add(Field.AGENT_DESCRIPTION);
    compressableFields.add(Field.EFFECTIVE_CONFIG);
    compressableFields.add(Field.REMOTE_CONFIG_STATUS);
    COMPRESSABLE_FIELDS = Collections.unmodifiableList(compressableFields);
  }

  public static OpampClientImpl create(RequestService requestService, OpampClientState state) {
    AgentToServerAppenders appenders =
        new AgentToServerAppenders(
            AgentDescriptionAppender.create(state.agentDescription),
            EffectiveConfigAppender.create(state.effectiveConfig),
            RemoteConfigStatusAppender.create(state.remoteConfigStatus),
            SequenceNumberAppender.create(state.sequenceNum),
            CapabilitiesAppender.create(state.capabilities),
            InstanceUidAppender.create(state.instanceUid),
            FlagsAppender.create(state.flags),
            AgentDisconnectAppender.create());
    return new OpampClientImpl(
        requestService, appenders, state, RecipeManager.create(CONSTANT_FIELDS));
  }

  private OpampClientImpl(
      RequestService requestService,
      AgentToServerAppenders appenders,
      OpampClientState state,
      RecipeManager recipeManager) {
    this.requestService = requestService;
    this.appenders = appenders;
    this.state = state;
    this.recipeManager = recipeManager;
  }

  @Override
  public void start(Callbacks callbacks) {
    if (hasStopped.get()) {
      throw new IllegalStateException("The client cannot start after it has been stopped.");
    }
    if (isRunning.compareAndSet(false, true)) {
      this.callbacks = callbacks;
      requestService.start(this, this);
      disableCompression();
      startObservingStateChange();
      requestService.sendRequest();
    } else {
      throw new IllegalStateException("The client has already been started");
    }
  }

  @Override
  public void stop() {
    if (isRunning.compareAndSet(true, false)) {
      hasStopped.set(true);
      stopObservingStateChange();
      prepareDisconnectRequest();
      requestService.stop();
    }
  }

  @Override
  public void setAgentDescription(AgentDescription agentDescription) {
    if (!state.agentDescription.get().equals(agentDescription)) {
      state.agentDescription.set(agentDescription);
      addFieldAndSend(Field.AGENT_DESCRIPTION);
    }
  }

  @Override
  public void setRemoteConfigStatus(RemoteConfigStatus remoteConfigStatus) {
    if (!state.remoteConfigStatus.get().equals(remoteConfigStatus)) {
      state.remoteConfigStatus.set(remoteConfigStatus);
      addFieldAndSend(Field.REMOTE_CONFIG_STATUS);
    }
  }

  @Override
  public void onConnectionSuccess() {
    getCallbacks().onConnect(this);
  }

  @Override
  public void onConnectionFailed(Throwable throwable) {
    getCallbacks().onConnectFailed(this, throwable);
  }

  @Override
  public void onRequestSuccess(Response response) {
    if (response == null) {
      return;
    }

    handleResponsePayload(response.getServerToAgent());
  }

  @Override
  public void onRequestFailed(Throwable throwable) {
    preserveFailedRequestRecipe();
    if (throwable instanceof OpampServerResponseException) {
      ServerErrorResponse errorResponse = ((OpampServerResponseException) throwable).errorResponse;
      getCallbacks().onErrorResponse(this, errorResponse);
    }
  }

  private void preserveFailedRequestRecipe() {
    RequestRecipe previous = recipeManager.previous();
    if (previous != null) {
      recipeManager.next().merge(previous);
    }
  }

  private void handleResponsePayload(ServerToAgent response) {
    int reportFullState = ServerToAgentFlags.ServerToAgentFlags_ReportFullState.getValue();
    if ((response.flags & reportFullState) == reportFullState) {
      disableCompression();
    }
    handleAgentIdentification(response);

    boolean notifyOnMessage = false;
    MessageData.Builder messageBuilder = MessageData.builder();

    if (response.remote_config != null) {
      notifyOnMessage = true;
      messageBuilder.setRemoteConfig(response.remote_config);
    }

    if (notifyOnMessage) {
      getCallbacks().onMessage(this, messageBuilder.build());
    }
  }

  private void handleAgentIdentification(ServerToAgent response) {
    if (response.agent_identification != null) {
      ByteString newInstanceUid = response.agent_identification.new_instance_uid;
      if (newInstanceUid.size() > 0) {
        state.instanceUid.set(newInstanceUid.toByteArray());
      }
    }
  }

  private void disableCompression() {
    recipeManager.next().addAllFields(COMPRESSABLE_FIELDS);
  }

  private void prepareDisconnectRequest() {
    recipeManager.next().addField(Field.AGENT_DISCONNECT);
  }

  @Nonnull
  private Callbacks getCallbacks() {
    return Objects.requireNonNull(callbacks);
  }

  @Override
  public Request get() {
    AgentToServer.Builder builder = new AgentToServer.Builder();
    for (Field field : recipeManager.next().build().getFields()) {
      appenders.getForField(field).appendTo(builder);
    }
    Request request = Request.create(builder.build());
    state.sequenceNum.increment();
    return request;
  }

  private void startObservingStateChange() {
    for (State<?> state : state.getAll()) {
      if (state instanceof ObservableState) {
        ((ObservableState<?>) state).addListener(this);
      }
    }
  }

  private void stopObservingStateChange() {
    for (State<?> state : state.getAll()) {
      if (state instanceof ObservableState) {
        ((ObservableState<?>) state).removeListener(this);
      }
    }
  }

  @Override
  public void onStateUpdate(Field type) {
    addFieldAndSend(type);
  }

  private void addFieldAndSend(Field field) {
    recipeManager.next().addField(field);
    requestService.sendRequest();
  }
}
