/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.opamp.client.internal.request.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.opentelemetry.opamp.client.internal.connectivity.websocket.OkHttpWebSocket;
import io.opentelemetry.opamp.client.internal.request.Request;
import io.opentelemetry.opamp.client.internal.request.delay.PeriodicDelay;
import io.opentelemetry.opamp.client.request.service.RequestService;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import mockwebserver3.junit5.StartStop;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;
import opamp.proto.AgentToServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class WebSocketRequestServiceIntegrationTest {
  @StartStop private final MockWebServer server = new MockWebServer();
  private final OkHttpClient client = new OkHttpClient();
  private final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);
  private final RequestService.Callback callback = mock();
  private final BlockingQueue<WebSocket> opened = new LinkedBlockingQueue<>();
  private final BlockingQueue<Integer> closed = new LinkedBlockingQueue<>();
  private final BlockingQueue<ByteString> messages = new LinkedBlockingQueue<>();
  private WebSocketRequestService requestService;

  private final WebSocketListener serverListener =
      new WebSocketListener() {
        @Override
        public void onOpen(WebSocket webSocket, Response response) {
          opened.add(webSocket);
        }

        @Override
        public void onClosing(WebSocket webSocket, int code, String reason) {
          webSocket.close(1000, null);
        }

        @Override
        public void onClosed(WebSocket webSocket, int code, String reason) {
          closed.add(code);
        }

        @Override
        public void onMessage(WebSocket webSocket, ByteString bytes) {
          messages.add(bytes);
        }
      };

  @AfterEach
  void tearDown() {
    if (requestService != null) {
      requestService.stop();
    }
    executor.shutdownNow();
    client.dispatcher().executorService().shutdownNow();
    client.connectionPool().evictAll();
  }

  @ParameterizedTest
  @ValueSource(ints = {1000, 1001, 4000})
  void reconnectsAfterServerClose(int code) throws InterruptedException, IOException {
    enqueueWebSocket();
    enqueueWebSocket();
    startService();

    WebSocket firstConnection = opened.poll(5, TimeUnit.SECONDS);
    assertThat(firstConnection).isNotNull();
    assertThat(firstConnection.close(code, "server closing")).isTrue();

    assertThat(closed.poll(5, TimeUnit.SECONDS)).isEqualTo(1000);
    assertThat(opened.poll(5, TimeUnit.SECONDS)).isNotNull().isNotSameAs(firstConnection);
    await()
        .atMost(Duration.ofSeconds(5))
        .untilAsserted(() -> verify(callback, times(2)).onConnectionSuccess());

    requestService.sendRequest();
    ByteString message = messages.poll(5, TimeUnit.SECONDS);
    assertThat(message).isNotNull();
    assertThat(message.getByte(0)).isZero();
    assertThat(AgentToServer.ADAPTER.decode(message.substring(1)).sequence_num).isEqualTo(10);

    requestService.stop();
    assertThat(closed.poll(5, TimeUnit.SECONDS)).isEqualTo(1000);
    await().atMost(Duration.ofSeconds(5)).until(executor::isTerminated);
    assertThat(executor.getTaskCount()).isEqualTo(1);
    assertThat(server.getRequestCount()).isEqualTo(2);
    verify(callback, never()).onConnectionFailed(any());
  }

  @Test
  void doesNotReconnectAfterStop() throws InterruptedException {
    enqueueWebSocket();
    startService();
    assertThat(opened.poll(5, TimeUnit.SECONDS)).isNotNull();
    await()
        .atMost(Duration.ofSeconds(5))
        .untilAsserted(() -> verify(callback).onConnectionSuccess());

    requestService.stop();

    assertThat(closed.poll(5, TimeUnit.SECONDS)).isEqualTo(1000);
    await().atMost(Duration.ofSeconds(5)).until(executor::isTerminated);
    assertThat(executor.getTaskCount()).isZero();
    assertThat(server.getRequestCount()).isEqualTo(1);
    verify(callback, never()).onConnectionFailed(any());
  }

  private void enqueueWebSocket() {
    server.enqueue(new MockResponse.Builder().webSocketUpgrade(serverListener).build());
  }

  private void startService() {
    requestService =
        new WebSocketRequestService(
            OkHttpWebSocket.create(server.url("/").toString(), client),
            PeriodicDelay.ofFixedDuration(Duration.ofMillis(10)),
            executor);
    requestService.start(
        callback, () -> Request.create(new AgentToServer.Builder().sequence_num(10).build()));
  }
}
