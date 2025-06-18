/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.opamp.client.internal.connectivity.websocket;

public interface WebSocket {
  void start(WebSocketListener listener);

  void send(byte[] request);

  void stop();
}
