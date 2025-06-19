/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.opamp.client.internal.connectivity.websocket;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class OkHttpWebSocket implements WebSocket {
  private final String url;
  private final OkHttpClient client;
  private final AtomicBoolean starting = new AtomicBoolean(false);
  private final AtomicBoolean closing = new AtomicBoolean(false);
  private final AtomicBoolean running = new AtomicBoolean(false);
  private final AtomicReference<okhttp3.WebSocket> webSocket = new AtomicReference<>();

  public static OkHttpWebSocket create(String url) {
    return create(url, new OkHttpClient());
  }

  public static OkHttpWebSocket create(String url, OkHttpClient client) {
    return new OkHttpWebSocket(url, client);
  }

  private OkHttpWebSocket(String url, OkHttpClient client) {
    this.url = url;
    this.client = client;
  }

  @Override
  public void open(Listener listener) {
    if (running.get()) {
      return;
    }
    if (starting.compareAndSet(false, true)) {
      okhttp3.Request request = new okhttp3.Request.Builder().url(url).build();
      webSocket.set(client.newWebSocket(request, new ListenerAdapter(listener)));
    }
  }

  @Override
  public boolean send(byte[] request) {
    if (!running.get()) {
      return false;
    }
    return getWebSocket().send(ByteString.of(request));
  }

  @Override
  public void close(int code, @Nullable String reason) {
    if (!running.get()) {
      return;
    }
    if (closing.compareAndSet(false, true)) {
      if (!getWebSocket().close(code, reason)) {
        closing.set(false);
        running.set(false);
      }
    }
  }

  private okhttp3.WebSocket getWebSocket() {
    return Objects.requireNonNull(webSocket.get());
  }

  private class ListenerAdapter extends WebSocketListener {
    private final Listener delegate;

    private ListenerAdapter(Listener delegate) {
      this.delegate = delegate;
    }

    @Override
    public void onOpen(@Nonnull okhttp3.WebSocket webSocket, @Nonnull Response response) {
      running.set(true);
      starting.set(false);
      delegate.onOpen();
    }

    @Override
    public void onClosing(@Nonnull okhttp3.WebSocket webSocket, int code, @Nonnull String reason) {
      running.set(false);
      closing.set(true);
    }

    @Override
    public void onClosed(@Nonnull okhttp3.WebSocket webSocket, int code, @Nonnull String reason) {
      running.set(false);
      closing.set(false);
      delegate.onClosed();
    }

    @Override
    public void onFailure(
        @Nonnull okhttp3.WebSocket webSocket, @Nonnull Throwable t, @Nullable Response response) {
      running.set(false);
      starting.set(false);
      closing.set(false);
      delegate.onFailure(t);
    }

    @Override
    public void onMessage(@Nonnull okhttp3.WebSocket webSocket, @Nonnull ByteString bytes) {
      delegate.onMessage(bytes.toByteArray());
    }
  }
}
