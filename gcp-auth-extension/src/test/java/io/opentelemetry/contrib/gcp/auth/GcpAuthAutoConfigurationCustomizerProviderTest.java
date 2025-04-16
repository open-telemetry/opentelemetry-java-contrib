/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.gcp.auth;

import static io.opentelemetry.contrib.gcp.auth.GcpAuthAutoConfigurationCustomizerProvider.GCP_USER_PROJECT_ID_KEY;
import static io.opentelemetry.contrib.gcp.auth.GcpAuthAutoConfigurationCustomizerProvider.QUOTA_USER_PROJECT_HEADER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Scope;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporterBuilder;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporterBuilder;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdkBuilder;
import io.opentelemetry.sdk.autoconfigure.internal.AutoConfigureUtil;
import io.opentelemetry.sdk.autoconfigure.internal.ComponentLoader;
import io.opentelemetry.sdk.autoconfigure.internal.SpiHelper;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigurationException;
import io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSpanExporterProvider;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GcpAuthAutoConfigurationCustomizerProviderTest {

  private static final String DUMMY_GCP_RESOURCE_PROJECT_ID = "my-gcp-resource-project-id";
  private static final String DUMMY_GCP_QUOTA_PROJECT_ID = "my-gcp-quota-project-id";

  @Mock private GoogleCredentials mockedGoogleCredentials;

  @Captor private ArgumentCaptor<Supplier<Map<String, String>>> headerSupplierCaptor;

  private static final ImmutableMap<String, String> otelProperties =
      ImmutableMap.of(
          "otel.traces.exporter",
          "otlp",
          "otel.metrics.exporter",
          "none",
          "otel.logs.exporter",
          "none",
          "otel.resource.attributes",
          "foo=bar");

  @BeforeEach
  public void setup() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  public void testCustomizerOtlpHttp() {
    // Set resource project system property
    System.setProperty(
        ConfigurableOption.GOOGLE_CLOUD_PROJECT.getSystemProperty(), DUMMY_GCP_RESOURCE_PROJECT_ID);
    // Prepare mocks
    prepareMockBehaviorForGoogleCredentials();
    OtlpHttpSpanExporter mockOtlpHttpSpanExporter = Mockito.mock(OtlpHttpSpanExporter.class);
    OtlpHttpSpanExporterBuilder otlpSpanExporterBuilder = OtlpHttpSpanExporter.builder();
    OtlpHttpSpanExporterBuilder spyOtlpHttpSpanExporterBuilder =
        Mockito.spy(otlpSpanExporterBuilder);
    Mockito.when(spyOtlpHttpSpanExporterBuilder.build()).thenReturn(mockOtlpHttpSpanExporter);

    Mockito.when(mockOtlpHttpSpanExporter.shutdown()).thenReturn(CompletableResultCode.ofSuccess());
    List<SpanData> exportedSpans = new ArrayList<>();
    Mockito.when(mockOtlpHttpSpanExporter.export(Mockito.anyCollection()))
        .thenAnswer(
            invocationOnMock -> {
              exportedSpans.addAll(invocationOnMock.getArgument(0));
              return CompletableResultCode.ofSuccess();
            });
    Mockito.when(mockOtlpHttpSpanExporter.toBuilder()).thenReturn(spyOtlpHttpSpanExporterBuilder);

    try (MockedStatic<GoogleCredentials> googleCredentialsMockedStatic =
        Mockito.mockStatic(GoogleCredentials.class)) {
      googleCredentialsMockedStatic
          .when(GoogleCredentials::getApplicationDefault)
          .thenReturn(mockedGoogleCredentials);

      OpenTelemetrySdk sdk = buildOpenTelemetrySdkWithExporter(mockOtlpHttpSpanExporter);
      generateTestSpan(sdk);
      CompletableResultCode code = sdk.shutdown();
      CompletableResultCode joinResult = code.join(10, TimeUnit.SECONDS);
      assertTrue(joinResult.isSuccess());

      Mockito.verify(mockOtlpHttpSpanExporter, Mockito.times(1)).toBuilder();
      Mockito.verify(spyOtlpHttpSpanExporterBuilder, Mockito.times(1))
          .setHeaders(headerSupplierCaptor.capture());
      assertEquals(2, headerSupplierCaptor.getValue().get().size());
      assertThat(authHeadersQuotaProjectIsPresent(headerSupplierCaptor.getValue().get())).isTrue();

      Mockito.verify(mockOtlpHttpSpanExporter, Mockito.atLeast(1)).export(Mockito.anyCollection());

      assertThat(exportedSpans)
          .hasSizeGreaterThan(0)
          .allSatisfy(
              spanData -> {
                assertThat(spanData.getResource().getAttributes().asMap())
                    .containsEntry(
                        AttributeKey.stringKey(GCP_USER_PROJECT_ID_KEY),
                        DUMMY_GCP_RESOURCE_PROJECT_ID)
                    .containsEntry(AttributeKey.stringKey("foo"), "bar");
                assertThat(spanData.getAttributes().asMap())
                    .containsKey(AttributeKey.longKey("work_loop"));
              });
    }
  }

  @Test
  public void testCustomizerOtlpGrpc() {
    // Set resource project system property
    System.setProperty(
        ConfigurableOption.GOOGLE_CLOUD_PROJECT.getSystemProperty(), DUMMY_GCP_RESOURCE_PROJECT_ID);
    // Prepare mocks
    prepareMockBehaviorForGoogleCredentials();
    OtlpGrpcSpanExporter mockOtlpGrpcSpanExporter = Mockito.mock(OtlpGrpcSpanExporter.class);
    OtlpGrpcSpanExporterBuilder spyOtlpGrpcSpanExporterBuilder =
        Mockito.spy(OtlpGrpcSpanExporter.builder());
    List<SpanData> exportedSpans = new ArrayList<>();
    configureGrpcMockExporters(
        mockOtlpGrpcSpanExporter, spyOtlpGrpcSpanExporterBuilder, exportedSpans);

    try (MockedStatic<GoogleCredentials> googleCredentialsMockedStatic =
        Mockito.mockStatic(GoogleCredentials.class)) {
      googleCredentialsMockedStatic
          .when(GoogleCredentials::getApplicationDefault)
          .thenReturn(mockedGoogleCredentials);

      OpenTelemetrySdk sdk = buildOpenTelemetrySdkWithExporter(mockOtlpGrpcSpanExporter);
      generateTestSpan(sdk);
      CompletableResultCode code = sdk.shutdown();
      CompletableResultCode joinResult = code.join(10, TimeUnit.SECONDS);
      assertTrue(joinResult.isSuccess());

      Mockito.verify(mockOtlpGrpcSpanExporter, Mockito.times(1)).toBuilder();
      Mockito.verify(spyOtlpGrpcSpanExporterBuilder, Mockito.times(1))
          .setHeaders(headerSupplierCaptor.capture());
      assertEquals(2, headerSupplierCaptor.getValue().get().size());
      assertThat(authHeadersQuotaProjectIsPresent(headerSupplierCaptor.getValue().get())).isTrue();

      Mockito.verify(mockOtlpGrpcSpanExporter, Mockito.atLeast(1)).export(Mockito.anyCollection());

      assertThat(exportedSpans)
          .hasSizeGreaterThan(0)
          .allSatisfy(
              spanData -> {
                assertThat(spanData.getResource().getAttributes().asMap())
                    .containsEntry(
                        AttributeKey.stringKey(GCP_USER_PROJECT_ID_KEY),
                        DUMMY_GCP_RESOURCE_PROJECT_ID)
                    .containsEntry(AttributeKey.stringKey("foo"), "bar");
                assertThat(spanData.getAttributes().asMap())
                    .containsKey(AttributeKey.longKey("work_loop"));
              });
    }
  }

  @Test
  public void testCustomizerFailWithMissingResourceProject() {
    OtlpGrpcSpanExporter mockOtlpGrpcSpanExporter = Mockito.mock(OtlpGrpcSpanExporter.class);
    try (MockedStatic<GoogleCredentials> googleCredentialsMockedStatic =
        Mockito.mockStatic(GoogleCredentials.class)) {
      googleCredentialsMockedStatic
          .when(GoogleCredentials::getApplicationDefault)
          .thenReturn(mockedGoogleCredentials);

      assertThrows(
          ConfigurationException.class,
          () -> buildOpenTelemetrySdkWithExporter(mockOtlpGrpcSpanExporter));
    }
  }

  @ParameterizedTest
  @MethodSource("provideQuotaBehaviorTestCases")
  @SuppressWarnings("CannotMockMethod")
  public void testQuotaProjectBehavior(QuotaProjectIdTestBehavior testCase) throws IOException {
    // Set resource project system property
    System.setProperty(
        ConfigurableOption.GOOGLE_CLOUD_PROJECT.getSystemProperty(), DUMMY_GCP_RESOURCE_PROJECT_ID);

    // Prepare request metadata
    AccessToken fakeAccessToken = new AccessToken("fake", Date.from(Instant.now()));
    ImmutableMap<String, List<String>> mockedRequestMetadata;
    if (testCase.getIsQuotaProjectPresentInMetadata()) {
      mockedRequestMetadata =
          ImmutableMap.of(
              "Authorization",
              Collections.singletonList("Bearer " + fakeAccessToken.getTokenValue()),
              QUOTA_USER_PROJECT_HEADER,
              Collections.singletonList(DUMMY_GCP_QUOTA_PROJECT_ID));
    } else {
      mockedRequestMetadata =
          ImmutableMap.of(
              "Authorization",
              Collections.singletonList("Bearer " + fakeAccessToken.getTokenValue()));
    }
    // mock credentials to return the prepared request metadata
    Mockito.when(mockedGoogleCredentials.getRequestMetadata()).thenReturn(mockedRequestMetadata);

    // configure environment according to test case
    String quotaProjectId = testCase.getUserSpecifiedQuotaProjectId(); // maybe empty string
    if (quotaProjectId != null) {
      // user specified a quota project id
      System.setProperty(
          ConfigurableOption.GOOGLE_CLOUD_QUOTA_PROJECT.getSystemProperty(), quotaProjectId);
    }

    // prepare mock exporter
    OtlpGrpcSpanExporter mockOtlpGrpcSpanExporter = Mockito.mock(OtlpGrpcSpanExporter.class);
    OtlpGrpcSpanExporterBuilder spyOtlpGrpcSpanExporterBuilder =
        Mockito.spy(OtlpGrpcSpanExporter.builder());
    List<SpanData> exportedSpans = new ArrayList<>();
    configureGrpcMockExporters(
        mockOtlpGrpcSpanExporter, spyOtlpGrpcSpanExporterBuilder, exportedSpans);

    try (MockedStatic<GoogleCredentials> googleCredentialsMockedStatic =
        Mockito.mockStatic(GoogleCredentials.class)) {
      googleCredentialsMockedStatic
          .when(GoogleCredentials::getApplicationDefault)
          .thenReturn(mockedGoogleCredentials);

      // Export telemetry to capture headers in the export calls
      OpenTelemetrySdk sdk = buildOpenTelemetrySdkWithExporter(mockOtlpGrpcSpanExporter);
      generateTestSpan(sdk);
      CompletableResultCode code = sdk.shutdown();
      CompletableResultCode joinResult = code.join(10, TimeUnit.SECONDS);
      assertTrue(joinResult.isSuccess());
      Mockito.verify(spyOtlpGrpcSpanExporterBuilder, Mockito.times(1))
          .setHeaders(headerSupplierCaptor.capture());

      // assert that the Authorization bearer token header is present
      Map<String, String> exportHeaders = headerSupplierCaptor.getValue().get();
      assertThat(exportHeaders).containsEntry("Authorization", "Bearer fake");

      if (testCase.getExpectedQuotaProjectInHeader() == null) {
        // there should be no user quota project header
        assertThat(exportHeaders).doesNotContainKey(QUOTA_USER_PROJECT_HEADER);
      } else {
        // there should be user quota project header with expected value
        assertThat(exportHeaders)
            .containsEntry(QUOTA_USER_PROJECT_HEADER, testCase.getExpectedQuotaProjectInHeader());
      }
    }
  }

  /**
   * Test cases specifying expected value for the user quota project header given the user input and
   * the current credentials state.
   *
   * <p>{@code null} for {@link QuotaProjectIdTestBehavior#getUserSpecifiedQuotaProjectId()}
   * indicates the case of user not specifying the quota project ID.
   *
   * <p>{@code null} value for {@link QuotaProjectIdTestBehavior#getExpectedQuotaProjectInHeader()}
   * indicates the expectation that the QUOTA_USER_PROJECT_HEADER should not be present in the
   * export headers.
   *
   * <p>{@code true} for {@link QuotaProjectIdTestBehavior#getIsQuotaProjectPresentInMetadata()}
   * indicates that the mocked credentials are configured to provide DUMMY_GCP_QUOTA_PROJECT_ID as
   * the quota project ID.
   */
  private static Stream<Arguments> provideQuotaBehaviorTestCases() {
    return Stream.of(
        // If quota project present in metadata, it will be used
        Arguments.of(
            QuotaProjectIdTestBehavior.builder()
                .setUserSpecifiedQuotaProjectId(DUMMY_GCP_QUOTA_PROJECT_ID)
                .setIsQuotaProjectPresentInMetadata(true)
                .setExpectedQuotaProjectInHeader(DUMMY_GCP_QUOTA_PROJECT_ID)
                .build()),
        Arguments.of(
            QuotaProjectIdTestBehavior.builder()
                .setUserSpecifiedQuotaProjectId("my-custom-quota-project-id")
                .setIsQuotaProjectPresentInMetadata(true)
                .setExpectedQuotaProjectInHeader(DUMMY_GCP_QUOTA_PROJECT_ID)
                .build()),
        // If quota project not present in request metadata, then user specified project is used
        Arguments.of(
            QuotaProjectIdTestBehavior.builder()
                .setUserSpecifiedQuotaProjectId(DUMMY_GCP_QUOTA_PROJECT_ID)
                .setIsQuotaProjectPresentInMetadata(false)
                .setExpectedQuotaProjectInHeader(DUMMY_GCP_QUOTA_PROJECT_ID)
                .build()),
        Arguments.of(
            QuotaProjectIdTestBehavior.builder()
                .setUserSpecifiedQuotaProjectId("my-custom-quota-project-id")
                .setIsQuotaProjectPresentInMetadata(false)
                .setExpectedQuotaProjectInHeader("my-custom-quota-project-id")
                .build()),
        // Testing for special edge case inputs
        // user-specified quota project is empty
        Arguments.of(
            QuotaProjectIdTestBehavior.builder()
                .setUserSpecifiedQuotaProjectId("") // user explicitly specifies empty
                .setIsQuotaProjectPresentInMetadata(true)
                .setExpectedQuotaProjectInHeader(DUMMY_GCP_QUOTA_PROJECT_ID)
                .build()),
        Arguments.of(
            QuotaProjectIdTestBehavior.builder()
                .setUserSpecifiedQuotaProjectId("")
                .setIsQuotaProjectPresentInMetadata(false)
                .setExpectedQuotaProjectInHeader(null)
                .build()),
        Arguments.of(
            QuotaProjectIdTestBehavior.builder()
                .setUserSpecifiedQuotaProjectId(null) // user omits specifying quota project
                .setIsQuotaProjectPresentInMetadata(true)
                .setExpectedQuotaProjectInHeader(DUMMY_GCP_QUOTA_PROJECT_ID)
                .build()),
        Arguments.of(
            QuotaProjectIdTestBehavior.builder()
                .setUserSpecifiedQuotaProjectId(null)
                .setIsQuotaProjectPresentInMetadata(false)
                .setExpectedQuotaProjectInHeader(null)
                .build()));
  }

  // Configure necessary behavior on the Grpc mock exporters to work
  // TODO: Potential improvement - make this work for Http exporter as well.
  private static void configureGrpcMockExporters(
      OtlpGrpcSpanExporter mockGrpcExporter,
      OtlpGrpcSpanExporterBuilder spyGrpcExporterBuilder,
      List<SpanData> exportedSpanContainer) {
    Mockito.when(spyGrpcExporterBuilder.build()).thenReturn(mockGrpcExporter);
    Mockito.when(mockGrpcExporter.shutdown()).thenReturn(CompletableResultCode.ofSuccess());
    Mockito.when(mockGrpcExporter.toBuilder()).thenReturn(spyGrpcExporterBuilder);
    Mockito.when(mockGrpcExporter.export(Mockito.anyCollection()))
        .thenAnswer(
            invocationOnMock -> {
              exportedSpanContainer.addAll(invocationOnMock.getArgument(0));
              return CompletableResultCode.ofSuccess();
            });
  }

  @AutoValue
  abstract static class QuotaProjectIdTestBehavior {
    // A null user specified quota represents the use case where user omits specifying quota
    @Nullable
    abstract String getUserSpecifiedQuotaProjectId();

    abstract boolean getIsQuotaProjectPresentInMetadata();

    // If expected quota project in header is null, the header entry should not be present in export
    @Nullable
    abstract String getExpectedQuotaProjectInHeader();

    static Builder builder() {
      return new AutoValue_GcpAuthAutoConfigurationCustomizerProviderTest_QuotaProjectIdTestBehavior
          .Builder();
    }

    @AutoValue.Builder
    abstract static class Builder {
      abstract Builder setUserSpecifiedQuotaProjectId(String quotaProjectId);

      abstract Builder setIsQuotaProjectPresentInMetadata(boolean quotaProjectPresentInMetadata);

      /**
       * Sets the expected quota project header value for the test case. A null value is allowed,
       * and it indicates that the header should not be present in the export request.
       *
       * @param expectedQuotaProjectInHeader the expected header value to match in the export
       *     headers.
       */
      abstract Builder setExpectedQuotaProjectInHeader(String expectedQuotaProjectInHeader);

      abstract QuotaProjectIdTestBehavior build();
    }
  }

  @SuppressWarnings("CannotMockMethod")
  private void prepareMockBehaviorForGoogleCredentials() {
    AccessToken fakeAccessToken = new AccessToken("fake", Date.from(Instant.now()));
    try {
      Mockito.when(mockedGoogleCredentials.getRequestMetadata())
          .thenReturn(
              ImmutableMap.of(
                  "Authorization",
                  Collections.singletonList("Bearer " + fakeAccessToken.getTokenValue()),
                  QUOTA_USER_PROJECT_HEADER,
                  Collections.singletonList(DUMMY_GCP_QUOTA_PROJECT_ID)));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private OpenTelemetrySdk buildOpenTelemetrySdkWithExporter(SpanExporter spanExporter) {
    SpiHelper spiHelper =
        SpiHelper.create(GcpAuthAutoConfigurationCustomizerProviderTest.class.getClassLoader());
    AutoConfiguredOpenTelemetrySdkBuilder builder =
        AutoConfiguredOpenTelemetrySdk.builder().addPropertiesSupplier(() -> otelProperties);
    AutoConfigureUtil.setComponentLoader(
        builder,
        new ComponentLoader() {
          @SuppressWarnings("unchecked")
          @Override
          public <T> List<T> load(Class<T> spiClass) {
            if (spiClass == ConfigurableSpanExporterProvider.class) {
              return Collections.singletonList(
                  (T)
                      new ConfigurableSpanExporterProvider() {
                        @Override
                        public SpanExporter createExporter(ConfigProperties configProperties) {
                          return spanExporter;
                        }

                        @Override
                        public String getName() {
                          return "otlp";
                        }
                      });
            }
            return spiHelper.load(spiClass);
          }
        });
    return builder.build().getOpenTelemetrySdk();
  }

  private static boolean authHeadersQuotaProjectIsPresent(Map<String, String> headers) {
    Set<Entry<String, String>> headerEntrySet = headers.entrySet();
    return headerEntrySet.contains(
            new SimpleEntry<>(
                QUOTA_USER_PROJECT_HEADER,
                GcpAuthAutoConfigurationCustomizerProviderTest.DUMMY_GCP_QUOTA_PROJECT_ID))
        && headerEntrySet.contains(new SimpleEntry<>("Authorization", "Bearer fake"));
  }

  private static void generateTestSpan(OpenTelemetrySdk openTelemetrySdk) {
    Span span = openTelemetrySdk.getTracer("test").spanBuilder("sample").startSpan();
    try (Scope ignored = span.makeCurrent()) {
      long workOutput = busyloop();
      span.setAttribute("work_loop", workOutput);
    } finally {
      span.end();
    }
  }

  // loop to simulate work done
  private static long busyloop() {
    Instant start = Instant.now();
    Instant end;
    long counter = 0;
    do {
      counter++;
      end = Instant.now();
    } while (Duration.between(start, end).toMillis() < 1000);
    return counter;
  }
}
