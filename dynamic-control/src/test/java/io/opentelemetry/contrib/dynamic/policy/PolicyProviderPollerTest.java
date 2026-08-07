/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.sun.net.httpserver.HttpServer;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import java.io.Closeable;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PolicyProviderPollerTest {

  @TempDir Path tempDir;
  private HttpServer httpServer;

  @AfterEach
  void tearDown() {
    if (httpServer != null) {
      httpServer.stop(0);
    }
    PolicyProviderPoller.reset();
  }

  @Test
  void registerStartsOneSharedSchedulerForMultipleTargets() throws Exception {
    PolicyProviderPoller.setGlobalPollInterval(Duration.ofMillis(10));
    Path firstFile = writePolicyFile("first-policies.txt", "trace-sampling=0.5");
    Path secondFile = writePolicyFile("second-policies.txt", "trace-sampling=0.25");

    Closeable first = PolicyProviderPoller.registerFile(firstFile, file -> {});
    Closeable second = PolicyProviderPoller.registerFile(secondFile, file -> {});

    assertThat(PolicyProviderPoller.getActiveTargetCount()).isEqualTo(2);
    assertThat(PolicyProviderPoller.isSchedulerRunning()).isTrue();
    assertThat(PolicyProviderPoller.getSchedulerStartCount()).isEqualTo(1);

    first.close();

    assertThat(PolicyProviderPoller.getActiveTargetCount()).isEqualTo(1);
    assertThat(PolicyProviderPoller.isSchedulerRunning()).isTrue();

    second.close();

    assertThat(PolicyProviderPoller.getActiveTargetCount()).isZero();
    assertThat(PolicyProviderPoller.isSchedulerRunning()).isFalse();
  }

  @Test
  void pollInvokesRegisteredFileTargetsWhenFileChanges() throws Exception {
    Path file = writePolicyFile("policies.txt", "trace-sampling=0.5");
    AtomicInteger pollCount = new AtomicInteger();
    PolicyProviderPoller.registerFile(file, changedFile -> pollCount.incrementAndGet());

    Files.write(file, Collections.singletonList("trace-sampling=0.75"));
    Files.setLastModifiedTime(
        file, FileTime.fromMillis(System.currentTimeMillis() + Duration.ofSeconds(2).toMillis()));

    PolicyProviderPoller.poll();

    assertThat(pollCount.get()).isEqualTo(1);
  }

  @Test
  void registerFileInvokesTargetOnlyWhenFileChanges() throws Exception {
    Path file = writePolicyFile("policies.txt", "trace-sampling=0.5");
    AtomicInteger pollCount = new AtomicInteger();
    PolicyProviderPoller.registerFile(file, changedFile -> pollCount.incrementAndGet());

    PolicyProviderPoller.poll();

    assertThat(pollCount.get()).isZero();

    Files.write(file, Collections.singletonList("trace-sampling=0.75"));
    Files.setLastModifiedTime(
        file, FileTime.fromMillis(System.currentTimeMillis() + Duration.ofSeconds(2).toMillis()));
    PolicyProviderPoller.poll();

    assertThat(pollCount.get()).isEqualTo(1);
  }

  @Test
  void registerUrlInvokesTargetOnInitialReadAndWhenResponseChanges() throws Exception {
    AtomicReference<String> responseBody = new AtomicReference<>("trace-sampling=0.5");
    AtomicInteger requestCount = new AtomicInteger();
    URI url = startHttpServer(responseBody, requestCount);
    AtomicInteger pollCount = new AtomicInteger();
    AtomicReference<String> changedBody = new AtomicReference<>();
    PolicyProviderPoller.registerUrl(
        url,
        (changedUrl, body) -> {
          pollCount.incrementAndGet();
          changedBody.set(new String(body, StandardCharsets.UTF_8));
        });

    assertThat(requestCount.get()).isZero();

    PolicyProviderPoller.poll();

    assertThat(requestCount.get()).isEqualTo(1);
    assertThat(pollCount.get()).isEqualTo(1);
    assertThat(changedBody.get()).isEqualTo("trace-sampling=0.5");

    responseBody.set("trace-sampling=0.75");
    PolicyProviderPoller.poll();

    assertThat(pollCount.get()).isEqualTo(2);
    assertThat(changedBody.get()).isEqualTo("trace-sampling=0.75");
  }

  @Test
  void registerUrlRetriesInitialReadWhenTargetFails() throws Exception {
    AtomicReference<String> responseBody = new AtomicReference<>("trace-sampling=0.5");
    AtomicInteger requestCount = new AtomicInteger();
    URI url = startHttpServer(responseBody, requestCount);
    AtomicInteger callbackAttempts = new AtomicInteger();
    AtomicReference<String> callbackBody = new AtomicReference<>();
    PolicyProviderPoller.registerUrl(
        url,
        (changedUrl, body) -> {
          callbackBody.set(new String(body, StandardCharsets.UTF_8));
          if (callbackAttempts.incrementAndGet() == 1) {
            throw new IllegalStateException("test callback failure");
          }
        });

    PolicyProviderPoller.poll();

    assertThat(callbackAttempts.get()).isEqualTo(1);
    assertThat(callbackBody.get()).isEqualTo("trace-sampling=0.5");

    PolicyProviderPoller.poll();

    assertThat(callbackAttempts.get()).isEqualTo(2);
    assertThat(callbackBody.get()).isEqualTo("trace-sampling=0.5");

    PolicyProviderPoller.poll();

    assertThat(callbackAttempts.get()).isEqualTo(2);
  }

  @Test
  void registerUrlRejectsNonHttpUrls() {
    assertThatThrownBy(
            () ->
                PolicyProviderPoller.registerUrl(
                    URI.create("file:///tmp/policies.txt"), (url, body) -> {}))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("URL must use http or https scheme");
  }

  @Test
  void rejectsInvalidPollInterval() {
    assertThatThrownBy(() -> PolicyProviderPoller.setGlobalPollInterval(Duration.ZERO))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("interval must be > 0");
  }

  @Test
  void configureAppliesConfiguredPollInterval() {
    ConfigProperties config = mock(ConfigProperties.class);
    when(config.getDuration(PolicyProviderPoller.POLL_INTERVAL_PROPERTY))
        .thenReturn(Duration.ofSeconds(5));

    PolicyProviderPoller.configure(config);

    assertThat(PolicyProviderPoller.getGlobalPollInterval()).isEqualTo(Duration.ofSeconds(5));
  }

  @Test
  void configureRetainsCurrentIntervalWhenPropertyIsAbsent() {
    ConfigProperties config = mock(ConfigProperties.class);
    PolicyProviderPoller.setGlobalPollInterval(Duration.ofSeconds(5));

    PolicyProviderPoller.configure(config);

    assertThat(PolicyProviderPoller.getGlobalPollInterval()).isEqualTo(Duration.ofSeconds(5));
  }

  @Test
  void configureIgnoresInvalidPollInterval() {
    ConfigProperties config = mock(ConfigProperties.class);
    when(config.getDuration(PolicyProviderPoller.POLL_INTERVAL_PROPERTY)).thenReturn(Duration.ZERO);

    PolicyProviderPoller.configure(config);

    assertThat(PolicyProviderPoller.getGlobalPollInterval()).isEqualTo(Duration.ofSeconds(30));
  }

  @Test
  void configureRejectsNullConfig() {
    assertThatThrownBy(() -> PolicyProviderPoller.configure(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("config cannot be null");
  }

  private Path writePolicyFile(String fileName, String line) throws Exception {
    Path file = tempDir.resolve(fileName);
    Files.write(file, Collections.singletonList(line));
    return file;
  }

  private URI startHttpServer(AtomicReference<String> responseBody, AtomicInteger requestCount)
      throws Exception {
    InetAddress loopback = InetAddress.getLoopbackAddress();
    httpServer = HttpServer.create(new InetSocketAddress(loopback, 0), 0);
    httpServer.createContext(
        "/policies",
        exchange -> {
          requestCount.incrementAndGet();
          byte[] body = responseBody.get().getBytes(StandardCharsets.UTF_8);
          exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
          exchange.sendResponseHeaders(200, body.length);
          exchange.getResponseBody().write(body);
          exchange.close();
        });
    httpServer.start();
    String host = loopback.getHostAddress();
    if (host.indexOf(':') >= 0) {
      host = "[" + host + "]";
    }
    return URI.create("http://" + host + ":" + httpServer.getAddress().getPort() + "/policies");
  }
}
