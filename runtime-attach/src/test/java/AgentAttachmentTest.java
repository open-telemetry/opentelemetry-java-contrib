/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.contrib.attach.RuntimeAttach;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

class AgentAttachmentTest {

  @BeforeAll
  static void before() {
    RuntimeAttach.attachJavaagentToCurrentJVM();
  }

  @Test
  void attachOtelAgent() throws IOException {

    List<String> httpHeaders = makeHttpCallAndReturnHeaders();

    String w3cTraceParentHeader = "traceparent";

    assertThat(httpHeaders).contains(w3cTraceParentHeader);
  }

  @Test
  @SetEnvironmentVariable(key = "OTEL_TRACES_EXPORTER", value = "logging")
  void attachOtelAgentWithLoggingExporter() throws IOException {

    List<String> httpHeaders = makeHttpCallAndReturnHeaders();

    String w3cTraceParentHeader = "traceparent";

    assertThat(httpHeaders).contains(w3cTraceParentHeader);
  }

  private static List<String> makeHttpCallAndReturnHeaders() throws IOException {
    try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
      HttpGet request = new HttpGet("https://httpbin.org/get");
      try (CloseableHttpResponse ignored = httpClient.execute(request)) {
        return Arrays.stream(request.getAllHeaders())
            .map(Header::getName)
            .collect(Collectors.toList());
      }
    }
  }
}
