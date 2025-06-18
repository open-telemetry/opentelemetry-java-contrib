/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.opamp.client.internal.connectivity.websocket;

public interface WebSocketListener {
  void onOpened(WebSocket webSocket);

  void onClosed(WebSocket webSocket);

  void onMessage(WebSocket webSocket, byte[] data);

  void onFailure(WebSocket webSocket, Throwable t);
}
