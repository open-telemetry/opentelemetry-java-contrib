/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.opamp.client.internal.connectivity.websocket;

import javax.annotation.Nullable;

public interface WebSocket {
  /**
   * Starts the websocket connection if it's not yet started or if it has been closed.
   *
   * @param listener Will receive events from the websocket connection.
   */
  void open(Listener listener);

  /**
   * Stops the websocket connection if running. Nothing will happen if it's already stopped.
   *
   * @param code Status code as defined by <a
   *     href="http://tools.ietf.org/html/rfc6455#section-7.4">Section 7.4 of RFC 6455</a>
   * @param reason Reason for shutting down, as explained in <a
   *     href="https://datatracker.ietf.org/doc/html/rfc6455#section-5.5.1">Section 5.5.1 of RFC
   *     6455</a>
   */
  void close(int code, @Nullable String reason);

  /**
   * Sends a message via the websocket connection.
   *
   * @param request The message payload.
   * @return {@code false} If the message can't be dispatched for any reason, whether the websocket
   *     isn't running, or the connection isn't established, or it's terminated. {@code true} if the
   *     message can get sent. Returning {@code true} doesn't guarantee that the message will arrive
   *     at the remote peer.
   */
  boolean send(byte[] request);

  interface Listener {

    /**
     * Called when the websocket connection is successfully established with the remote peer. The
     * client may start sending messages after this method is called.
     */
    void onOpen();

    /**
     * Called when the closing handshake has started. No further messages will be sent after this
     * method call.
     */
    void onClosing();

    /** Called when the connection is terminated and no further messages can be transmitted. */
    void onClosed();

    /**
     * Called when receiving a message from the remote peer.
     *
     * @param data The payload sent by the remote peer.
     */
    void onMessage(byte[] data);

    /**
     * Called when the connection is closed or cannot be established due to an error. No messages
     * can be transmitted after this method is called.
     *
     * @param t The error.
     */
    void onFailure(Throwable t);
  }
}
