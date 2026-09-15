/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpServer;
import io.opentelemetry.contrib.dynamic.policy.registry.PolicySourceMappingConfig;
import io.opentelemetry.contrib.dynamic.policy.source.SourceFormat;
import io.opentelemetry.contrib.dynamic.policy.source.SourceKind;
import io.opentelemetry.contrib.dynamic.policy.tracesampling.TraceSamplingRatePolicy;
import io.opentelemetry.contrib.dynamic.policy.tracesampling.TraceSamplingValidator;
import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class HttpPolicyProviderTest {

  private HttpServer httpServer;

  @AfterEach
  void tearDown() {
    if (httpServer != null) {
      httpServer.stop(0);
    }
    HttpPolicyProvider.resetForTest();
  }

  @Test
  void fetchPoliciesReturnsCurrentPoliciesWithoutReadingEndpoint() throws Exception {
    AtomicInteger requestCount = new AtomicInteger();
    URI endpoint = startHttpServer(new AtomicReference<>("trace-sampling=0.5"), requestCount);
    HttpPolicyProvider provider = provider(endpoint, SourceFormat.KEYVALUE);

    List<TelemetryPolicy> policies = provider.fetchPolicies();

    assertThat(policies).isEmpty();
    assertThat(requestCount.get()).isZero();
  }

  @Test
  void firstPollParsesKeyValueResponseBody() throws Exception {
    AtomicInteger requestCount = new AtomicInteger();
    URI endpoint = startHttpServer(new AtomicReference<>("trace-sampling=0.5"), requestCount);
    HttpPolicyProvider provider = provider(endpoint, SourceFormat.KEYVALUE);
    AtomicReference<List<TelemetryPolicy>> latestPolicies = new AtomicReference<>();
    Closeable watch = provider.startWatching(latestPolicies::set);

    assertThat(requestCount.get()).isZero();

    PolicyProviderPoller.poll();

    assertThat(requestCount.get()).isEqualTo(1);
    List<TelemetryPolicy> policies = latestPolicies.get();
    assertThat(policies).hasSize(1);
    TraceSamplingRatePolicy policy = (TraceSamplingRatePolicy) policies.get(0);
    assertThat(policy.getProbability()).isEqualTo(0.5);
    assertThat(policy.getSourceKind()).isEqualTo(SourceKind.HTTP);
    assertThat(provider.fetchPolicies()).isSameAs(policies);
    watch.close();
  }

  @Test
  void firstPollParsesJsonKeyValueResponseBody() throws Exception {
    URI endpoint =
        startHttpServer(new AtomicReference<>("[{\"other-policy\":1},{\"trace-sampling\":0.25}]"));
    HttpPolicyProvider provider = provider(endpoint, SourceFormat.JSONKEYVALUE);
    AtomicReference<List<TelemetryPolicy>> latestPolicies = new AtomicReference<>();
    Closeable watch = provider.startWatching(latestPolicies::set);

    PolicyProviderPoller.poll();

    List<TelemetryPolicy> policies = latestPolicies.get();
    assertThat(policies).hasSize(1);
    TraceSamplingRatePolicy policy = (TraceSamplingRatePolicy) policies.get(0);
    assertThat(policy.getProbability()).isEqualTo(0.25);
    assertThat(policy.getSourceKind()).isEqualTo(SourceKind.HTTP);
    watch.close();
  }

  /*
   * TODO: Uncomment after full-policy JSON source support is merged.
   *
   * @Test
   * void firstPollParsesFullPolicyJsonResponseBody() throws Exception {
   *   URI endpoint =
   *       startHttpServer(
   *           new AtomicReference<>(
   *               "[{\"other-policy\":1},"
   *                   + "{\"id\":\"trace-sampling\","
   *                   + "\"name\":\"Trace sampling rate\","
   *                   + "\"trace\":{\"match\":[{\"trace_field\":\"trace_id\",\"exists\":true}],"
   *                   + "\"keep\":{\"ratio\":0.25}}}]"));
   *   HttpPolicyProvider provider = provider(endpoint, SourceFormat.JSONKEYVALUE);
   *   AtomicReference<List<TelemetryPolicy>> latestPolicies = new AtomicReference<>();
   *   Closeable watch = provider.startWatching(latestPolicies::set);
   *
   *   PolicyProviderPoller.poll();
   *
   *   List<TelemetryPolicy> policies = latestPolicies.get();
   *   assertThat(policies).hasSize(1);
   *   TraceSamplingRatePolicy policy = (TraceSamplingRatePolicy) policies.get(0);
   *   assertThat(policy.getProbability()).isEqualTo(0.25);
   *   assertThat(policy.getSourceKind()).isEqualTo(SourceKind.HTTP);
   *   watch.close();
   * }
   */

  @Test
  void startWatchingReloadsWhenResponseBodyChanges() throws Exception {
    AtomicReference<String> responseBody = new AtomicReference<>("trace-sampling=0.5");
    URI endpoint = startHttpServer(responseBody);
    HttpPolicyProvider provider = provider(endpoint, SourceFormat.KEYVALUE);
    AtomicInteger updateCount = new AtomicInteger();
    AtomicReference<List<TelemetryPolicy>> latestPolicies = new AtomicReference<>();
    Closeable watch =
        provider.startWatching(
            policies -> {
              updateCount.incrementAndGet();
              latestPolicies.set(policies);
            });

    PolicyProviderPoller.poll();
    responseBody.set("trace-sampling=0.75");
    PolicyProviderPoller.poll();

    assertThat(updateCount.get()).isEqualTo(2);
    assertThat(latestPolicies.get()).hasSize(1);
    TraceSamplingRatePolicy policy = (TraceSamplingRatePolicy) latestPolicies.get().get(0);
    assertThat(policy.getProbability()).isEqualTo(0.75);
    watch.close();
  }

  @Test
  void rejectsNonHttpEndpoint() {
    assertThatThrownBy(() -> provider(URI.create("file:///tmp/policies"), SourceFormat.KEYVALUE))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("HTTP policy endpoint must use http or https scheme");
  }

  private URI startHttpServer(AtomicReference<String> responseBody) throws IOException {
    return startHttpServer(responseBody, new AtomicInteger());
  }

  private URI startHttpServer(AtomicReference<String> responseBody, AtomicInteger requestCount)
      throws IOException {
    InetAddress loopback = InetAddress.getLoopbackAddress();
    httpServer = HttpServer.create(new InetSocketAddress(loopback, 0), 0);
    httpServer.createContext(
        "/policies",
        exchange -> {
          requestCount.incrementAndGet();
          byte[] body = responseBody.get().getBytes(StandardCharsets.UTF_8);
          exchange.sendResponseHeaders(200, body.length);
          try (OutputStream response = exchange.getResponseBody()) {
            response.write(body);
          }
        });
    httpServer.start();
    String host = loopback.getHostAddress();
    if (host.indexOf(':') >= 0) {
      host = "[" + host + "]";
    }
    return URI.create("http://" + host + ":" + httpServer.getAddress().getPort() + "/policies");
  }

  private static HttpPolicyProvider provider(URI endpoint, SourceFormat format) {
    return new HttpPolicyProvider(
        endpoint,
        format,
        Collections.singletonList(
            new PolicySourceMappingConfig(
                TraceSamplingRatePolicy.POLICY_TYPE, TraceSamplingRatePolicy.POLICY_TYPE)),
        Collections.singletonList(new TraceSamplingValidator()));
  }
}
