/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.opamp.client.internal;

import io.opentelemetry.opamp.client.internal.response.MessageData;
import javax.annotation.Nullable;
import opamp.proto.AgentDescription;
import opamp.proto.RemoteConfigStatus;
import opamp.proto.ServerErrorResponse;

public interface OpampClient {

  static OpampClientBuilder builder() {
    return new OpampClientBuilder();
  }

  /**
   * Starts the client and begin attempts to connect to the Server. Once connection is established
   * the client will attempt to maintain it by reconnecting if the connection is lost. All failed
   * connection attempts will be reported via {@link Callbacks#onConnectFailed(Throwable)} callback.
   *
   * <p>This method does not wait until the connection to the Server is established and will likely
   * return before the connection attempts are even made.
   *
   * <p>This method may be called only once.
   *
   * @param callbacks The Callback to which the Client will notify about any Server requests and
   *     responses.
   */
  void start(Callbacks callbacks);

  /**
   * Stops the client. May be called only after {@link #start(Callbacks)}. May be called only once.
   * After this call returns successfully it is guaranteed that no callbacks will be called. Once
   * stopped, the client cannot be started again.
   */
  void stop();

  /**
   * Sets attributes of the Agent. The attributes will be included in the next status report sent to
   * the Server. When called after {@link #start(Callbacks)}, the attributes will be included in the
   * next outgoing status report. This is typically used by Agents which allow their
   * AgentDescription to change dynamically while the OpAMPClient is started. May be also called
   * from {@link Callbacks#onMessage(MessageData)}.
   *
   * @param agentDescription The new agent description.
   */
  void setAgentDescription(AgentDescription agentDescription);

  /**
   * Sets the current remote config status which will be sent in the next agent to server request.
   *
   * @param remoteConfigStatus The new remote config status.
   */
  void setRemoteConfigStatus(RemoteConfigStatus remoteConfigStatus);

  interface Callbacks {
    /**
     * Called when the connection is successfully established to the Server. May be called after
     * {@link #start(Callbacks)} is called and every time a connection is established to the Server.
     * For WebSocket clients this is called after the handshake is completed without any error. For
     * HTTP clients this is called for any request if the response status is OK.
     */
    void onConnect();

    /**
     * Called when the connection to the Server cannot be established. May be called after {@link
     * #start(Callbacks)} is called and tries to connect to the Server. May also be called if the
     * connection is lost and reconnection attempt fails.
     *
     * @param throwable The exception.
     */
    void onConnectFailed(@Nullable Throwable throwable);

    /**
     * Called when the Server reports an error in response to some previously sent request. Useful
     * for logging purposes. The Agent should not attempt to process the error by reconnecting or
     * retrying previous operations. The client handles the ErrorResponse_UNAVAILABLE case
     * internally by performing retries as necessary.
     *
     * @param errorResponse The error returned by the Server.
     */
    void onErrorResponse(ServerErrorResponse errorResponse);

    /**
     * Called when the Agent receives a message that needs processing. See {@link MessageData}
     * definition for the data that may be available for processing. During onMessage execution the
     * {@link OpampClient} functions that change the status of the client may be called, e.g. if
     * RemoteConfig is processed then {@link #setRemoteConfigStatus(opamp.proto.RemoteConfigStatus)}
     * should be called to reflect the processing result. These functions may also be called after
     * onMessage returns. This is advisable if processing can take a long time. In that case
     * returning quickly is preferable to avoid blocking the {@link OpampClient}.
     *
     * @param messageData The server response data that needs processing.
     */
    void onMessage(MessageData messageData);
  }
}
