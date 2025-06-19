package io.opentelemetry.opamp.client.internal.connectivity.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocketListener;
import okio.ByteString;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OkHttpWebSocketTest {
  @Mock private OkHttpClient client;
  @Mock private okhttp3.WebSocket okHttpWebSocket;
  @Mock private WebSocket.Listener listener;
  @Captor private ArgumentCaptor<Request> requestCaptor;
  @Captor private ArgumentCaptor<WebSocketListener> listenerCaptor;
  private static final String URL = "ws://some.server";
  private OkHttpWebSocket webSocket;

  @BeforeEach
  void setUp() {
    webSocket = OkHttpWebSocket.create(URL, client);
    when(client.newWebSocket(any(), any())).thenReturn(okHttpWebSocket);
  }

  @Test
  void validateOpen() {
    // Assert websocket created
    openAndCaptureArguments();
    assertThat(requestCaptor.getValue().url().host()).isEqualTo("some.server");

    // Assert further calls to open won't do anything
    webSocket.open(listener);
    verifyNoMoreInteractions(client);

    // When connectivity succeeds, open calls won't do anything.
    callOnOpen();
    webSocket.open(listener);
    verifyNoMoreInteractions(client);

    // When connectivity fails, allow future open calls
    clearInvocations(client);
    callOnFailure();
    openAndCaptureArguments();
    assertThat(requestCaptor.getValue().url().host()).isEqualTo("some.server");
  }

  @Test
  void validateSend() {
    byte[] payload = new byte[1];

    // Before opening
    assertThat(webSocket.send(payload)).isFalse();

    // After opening successfully
    when(okHttpWebSocket.send(any(ByteString.class))).thenReturn(true);
    openAndCaptureArguments();
    callOnOpen();
    assertThat(webSocket.send(payload)).isTrue();
    verify(okHttpWebSocket).send(ByteString.of(payload));

    // After failing
    callOnFailure();
    assertThat(webSocket.send(payload)).isFalse();
    verifyNoMoreInteractions(okHttpWebSocket);
  }

  @Test
  void validateClose() {
    openAndCaptureArguments();

    callOnOpen();
    webSocket.close(123, "something");
    verify(okHttpWebSocket).close(123, "something");

    // Validate calling it again
    webSocket.close(1, null);
    verifyNoMoreInteractions(okHttpWebSocket);

    // Once closed, it should be possible to reopen it.
    clearInvocations(client);
    callOnClosed();
    openAndCaptureArguments();
  }

  @Test
  void validateOnClosing() {
    openAndCaptureArguments();

    callOnOpen();
    callOnClosing();

    // Validate calling after onClosing
    webSocket.close(1, null);
    verifyNoInteractions(okHttpWebSocket);
  }

  @Test
  void validateOnMessage() {
    byte[] payload = new byte[1];
    openAndCaptureArguments();

    listenerCaptor.getValue().onMessage(mock(), ByteString.of(payload));
    verify(listener).onMessage(payload);
  }

  private void callOnOpen() {
    listenerCaptor.getValue().onOpen(mock(), mock());
    verify(listener).onOpen();
  }

  private void callOnClosed() {
    listenerCaptor.getValue().onClosed(mock(), 0, "");
    verify(listener).onClosed();
  }

  private void callOnClosing() {
    listenerCaptor.getValue().onClosing(mock(), 0, "");
  }

  private void callOnFailure() {
    Throwable t = mock();
    listenerCaptor.getValue().onFailure(mock(), t, mock());
    verify(listener).onFailure(t);
  }

  private void openAndCaptureArguments() {
    webSocket.open(listener);
    verify(client).newWebSocket(requestCaptor.capture(), listenerCaptor.capture());
  }
}
