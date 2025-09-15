/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.opamp.client.internal.connectivity.websocket;

import static java.util.Objects.requireNonNull;

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
  private final AtomicReference<Status> status = new AtomicReference<>(Status.NOT_RUNNING);
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
    if (status.compareAndSet(Status.NOT_RUNNING, Status.STARTING)) {
      okhttp3.Request request = new okhttp3.Request.Builder().url(url).build();
      webSocket.set(client.newWebSocket(request, new ListenerAdapter(listener)));
    }
  }

  @Override
  public boolean send(byte[] request) {
    if (status.get() != Status.RUNNING) {
      return false;
    }
    return getWebSocket().send(ByteString.of(request));
  }

  @Override
  public void close(int code, @Nullable String reason) {
    if (status.compareAndSet(Status.RUNNING, Status.CLOSING)) {
      try {
        if (!getWebSocket().close(code, reason)) {
          status.set(Status.NOT_RUNNING);
        }
      } catch (IllegalArgumentException e) {
        status.set(Status.RUNNING);
        // Re-throwing as this error happens due to a caller error.
        throw e;
      }
    }
  }

  private okhttp3.WebSocket getWebSocket() {
    return requireNonNull(webSocket.get());
  }

  private class ListenerAdapter extends WebSocketListener {
    private final Listener delegate;

    private ListenerAdapter(Listener delegate) {
      this.delegate = delegate;
    }

    @Override
    public void onOpen(@Nonnull okhttp3.WebSocket webSocket, @Nonnull Response response) {
      status.set(Status.RUNNING);
      delegate.onOpen();
    }

    @Override
    public void onClosing(@Nonnull okhttp3.WebSocket webSocket, int code, @Nonnull String reason) {
      status.set(Status.CLOSING);
      delegate.onClosing();
    }

    @Override
    public void onClosed(@Nonnull okhttp3.WebSocket webSocket, int code, @Nonnull String reason) {
      status.set(Status.NOT_RUNNING);
      delegate.onClosed();
    }

    @Override
    public void onFailure(
        @Nonnull okhttp3.WebSocket webSocket, @Nonnull Throwable t, @Nullable Response response) {
      status.set(Status.NOT_RUNNING);
      delegate.onFailure(t);
    }

    @Override
    public void onMessage(@Nonnull okhttp3.WebSocket webSocket, @Nonnull ByteString bytes) {
      delegate.onMessage(bytes.toByteArray());
    }
  }

  enum Status {
    NOT_RUNNING,
    STARTING,
    CLOSING,
    RUNNING
  }
}
