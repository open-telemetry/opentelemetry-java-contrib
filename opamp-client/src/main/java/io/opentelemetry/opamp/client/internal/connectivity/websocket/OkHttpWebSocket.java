/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.opamp.client.internal.connectivity.websocket;

import okhttp3.OkHttpClient;
import okhttp3.Response;
import okio.ByteString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class OkHttpWebSocket extends okhttp3.WebSocketListener implements WebSocket {
  private final OkHttpClient client;
  private final String url;
  private WebSocketListener listener;
  private okhttp3.WebSocket webSocket;

  public static OkHttpWebSocket create(String url) {
    OkHttpClient client = new OkHttpClient();
    return new OkHttpWebSocket(client, url);
  }

  public OkHttpWebSocket(OkHttpClient client, String url) {
    this.client = client;
    this.url = url;
  }

  @Override
  public void start(WebSocketListener listener) {
    this.listener = listener;
    okhttp3.Request request = new okhttp3.Request.Builder().url(url).build();
    webSocket = client.newWebSocket(request, this);
  }

  @Override
  public void send(byte[] request) {
    webSocket.send(ByteString.of(request));
  }

  @Override
  public void stop() {
    webSocket.cancel();
  }

  @Override
  public void onOpen(@NotNull okhttp3.WebSocket webSocket, @NotNull Response response) {
    listener.onOpened(this);
  }

  @Override
  public void onClosed(@NotNull okhttp3.WebSocket webSocket, int code, @NotNull String reason) {
    listener.onClosed(this);
  }

  @Override
  public void onFailure(
      @NotNull okhttp3.WebSocket webSocket, @NotNull Throwable t, @Nullable Response response) {
    listener.onFailure(this, t);
  }

  @Override
  public void onMessage(@NotNull okhttp3.WebSocket webSocket, @NotNull ByteString bytes) {
    listener.onMessage(this, bytes.toByteArray());
  }
}
