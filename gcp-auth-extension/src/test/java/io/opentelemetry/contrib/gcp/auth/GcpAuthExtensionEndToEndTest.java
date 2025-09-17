/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.gcp.auth;

import static io.opentelemetry.contrib.gcp.auth.GcpAuthAutoConfigurationCustomizerProvider.GCP_USER_PROJECT_ID_KEY;
import static io.opentelemetry.contrib.gcp.auth.GcpAuthAutoConfigurationCustomizerProvider.QUOTA_USER_PROJECT_HEADER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.stop.Stop.stopQuietly;

import com.google.protobuf.InvalidProtocolBufferException;
import io.opentelemetry.contrib.gcp.auth.springapp.Application;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.trace.v1.ResourceSpans;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.Body;
import org.mockserver.model.Headers;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.JsonBody;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(
    classes = {Application.class},
    webEnvironment = WebEnvironment.RANDOM_PORT)
class GcpAuthExtensionEndToEndTest {

  @LocalServerPort private int testApplicationPort; // port at which the spring app is running

  @Autowired private TestRestTemplate template;

  // The port at which the backend server will receive telemetry
  private static final int EXPORTER_ENDPOINT_PORT = 4318;
  // The port at which the mock GCP OAuth 2.0 server will run
  private static final int MOCK_GCP_OAUTH2_PORT = 8090;

  // Backend server to which the application under test will export traces
  // the export config is specified in the build.gradle file.
  private static ClientAndServer backendServer;

  // Mock server to intercept calls to the GCP OAuth 2.0 server and provide fake credentials
  private static ClientAndServer mockGcpOAuth2Server;

  private static final String DUMMY_GCP_QUOTA_PROJECT = System.getenv("GOOGLE_CLOUD_QUOTA_PROJECT");
  private static final String DUMMY_GCP_PROJECT = System.getProperty("google.cloud.project");

  @BeforeAll
  public static void setup() throws NoSuchAlgorithmException, KeyManagementException {
    // Setup proxy host(s)
    System.setProperty("http.proxyHost", "localhost");
    System.setProperty("http.proxyPort", MOCK_GCP_OAUTH2_PORT + "");
    System.setProperty("https.proxyHost", "localhost");
    System.setProperty("https.proxyPort", MOCK_GCP_OAUTH2_PORT + "");
    System.setProperty("http.nonProxyHost", "localhost");
    System.setProperty("https.nonProxyHost", "localhost");

    // Disable SSL validation for integration test
    // The OAuth2 token validation requires SSL validation
    disableSSLValidation();

    // Set up mock OTLP backend server to which traces will be exported
    backendServer = ClientAndServer.startClientAndServer(EXPORTER_ENDPOINT_PORT);
    backendServer.when(request()).respond(response().withStatusCode(200));
    String accessTokenResponse =
        "{\"access_token\": \"fake.access_token\",\"expires_in\": 3600, \"token_type\": \"Bearer\"}";

    mockGcpOAuth2Server = ClientAndServer.startClientAndServer(MOCK_GCP_OAUTH2_PORT);

    MockServerClient mockServerClient =
        new MockServerClient("localhost", MOCK_GCP_OAUTH2_PORT).withSecure(true);

    // mock the token refresh - always respond with 200
    mockServerClient
        .when(request().withMethod("POST").withPath("/token"))
        .respond(
            response()
                .withStatusCode(200)
                .withHeader("Content-Type", "application/json")
                .withBody(new JsonBody(accessTokenResponse)));
  }

  @AfterAll
  public static void teardown() {
    // Stop the backend server
    stopQuietly(backendServer);
    stopQuietly(mockGcpOAuth2Server);
  }

  @Test
  void authExtensionSmokeTest() {
    template.getForEntity(
        URI.create("http://localhost:" + testApplicationPort + "/ping"), String.class);

    await()
        .atMost(Duration.ofSeconds(10))
        .untilAsserted(
            () -> {
              HttpRequest[] requests = backendServer.retrieveRecordedRequests(request());
              List<Headers> extractedHeaders = extractHeadersFromRequests(requests);
              verifyRequestHeaders(extractedHeaders);

              List<ResourceSpans> extractedResourceSpans =
                  extractResourceSpansFromRequests(requests);
              verifyResourceAttributes(extractedResourceSpans);
            });
  }

  // Helper methods

  private static void disableSSLValidation()
      throws NoSuchAlgorithmException, KeyManagementException {
    TrustManager[] trustAllCerts =
        new TrustManager[] {
          new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) {}

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) {}

            @Override
            public X509Certificate[] getAcceptedIssuers() {
              return null;
            }
          }
        };
    SSLContext sc = SSLContext.getInstance("SSL");
    sc.init(null, trustAllCerts, new java.security.SecureRandom());
    HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
  }

  private static void verifyResourceAttributes(List<ResourceSpans> extractedResourceSpans) {
    extractedResourceSpans.forEach(
        resourceSpan ->
            assertThat(resourceSpan.getResource().getAttributesList())
                .contains(
                    KeyValue.newBuilder()
                        .setKey(GCP_USER_PROJECT_ID_KEY)
                        .setValue(AnyValue.newBuilder().setStringValue(DUMMY_GCP_PROJECT))
                        .build()));
  }

  private static void verifyRequestHeaders(List<Headers> extractedHeaders) {
    assertThat(extractedHeaders).isNotEmpty();
    // verify if extension added the required headers
    extractedHeaders.forEach(
        headers -> {
          assertThat(headers.containsEntry(QUOTA_USER_PROJECT_HEADER, DUMMY_GCP_QUOTA_PROJECT))
              .isTrue();
          assertThat(headers.containsEntry("Authorization", "Bearer fake.access_token")).isTrue();
        });
  }

  private static List<Headers> extractHeadersFromRequests(HttpRequest[] requests) {
    return Arrays.stream(requests).map(HttpRequest::getHeaders).collect(Collectors.toList());
  }

  /**
   * Extract resource spans from http requests received by a telemetry collector.
   *
   * @param requests Request received by a http server trace collector
   * @return spans extracted from the request body
   */
  private static List<ResourceSpans> extractResourceSpansFromRequests(HttpRequest[] requests) {
    return Arrays.stream(requests)
        .map(HttpRequest::getBody)
        .map(GcpAuthExtensionEndToEndTest::getExportTraceServiceRequest)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .flatMap(
            exportTraceServiceRequest -> exportTraceServiceRequest.getResourceSpansList().stream())
        .collect(Collectors.toList());
  }

  private static Optional<ExportTraceServiceRequest> getExportTraceServiceRequest(Body<?> body) {
    try {
      return Optional.ofNullable(ExportTraceServiceRequest.parseFrom(body.getRawBytes()));
    } catch (InvalidProtocolBufferException e) {
      return Optional.empty();
    }
  }
}
