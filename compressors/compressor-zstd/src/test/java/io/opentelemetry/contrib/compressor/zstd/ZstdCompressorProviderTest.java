/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.compressor.zstd;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.luben.zstd.ZstdInputStream;
import com.linecorp.armeria.common.HttpRequest;
import com.linecorp.armeria.common.HttpResponse;
import com.linecorp.armeria.common.HttpStatus;
import com.linecorp.armeria.common.MediaType;
import com.linecorp.armeria.server.ServerBuilder;
import com.linecorp.armeria.testing.junit5.server.ServerExtension;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.logs.Severity;
import io.opentelemetry.exporter.otlp.http.logs.OtlpHttpLogRecordExporter;
import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceRequest;
import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceResponse;
import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.common.v1.InstrumentationScope;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.logs.v1.ResourceLogs;
import io.opentelemetry.proto.logs.v1.SeverityNumber;
import io.opentelemetry.sdk.common.InstrumentationScopeInfo;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.testing.logs.TestLogRecordData;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.Collections;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class ZstdCompressorProviderTest {

  private static final HttpResponse SUCCESS =
      HttpResponse.of(
          HttpStatus.OK,
          MediaType.parse("application/x-protobuf"),
          ExportLogsServiceResponse.getDefaultInstance().toByteArray());

  private static final ConcurrentLinkedQueue<HttpRequest> httpRequests =
      new ConcurrentLinkedQueue<>();
  private static final ConcurrentLinkedQueue<ResourceLogs> exportedTelemetry =
      new ConcurrentLinkedQueue<>();

  @RegisterExtension
  static final ServerExtension server =
      new ServerExtension() {
        @Override
        protected void configure(ServerBuilder sb) {
          sb.service(
              "/v1/logs",
              (ctx, req) -> {
                httpRequests.add(ctx.request());
                return HttpResponse.of(
                    req.aggregate()
                        .thenApply(
                            aggReq -> {
                              byte[] payload = aggReq.content().array();
                              try {
                                if (req.headers().contains("content-encoding", "zstd")) {
                                  ZstdInputStream is =
                                      new ZstdInputStream(new ByteArrayInputStream(payload));
                                  ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                  for (int result = is.read(); result != -1; result = is.read()) {
                                    baos.write((byte) result);
                                  }
                                  payload = baos.toByteArray();
                                }
                                ExportLogsServiceRequest parsed =
                                    ExportLogsServiceRequest.parseFrom(payload);
                                exportedTelemetry.addAll(parsed.getResourceLogsList());
                                return SUCCESS;
                              } catch (IOException e) {
                                throw new UncheckedIOException(e);
                              }
                            }));
              });
          sb.http(0);
        }
      };

  @Test
  void exporterWithZstd() {
    try (OtlpHttpLogRecordExporter exporter =
        OtlpHttpLogRecordExporter.builder()
            .setEndpoint(server.httpUri() + "/v1/logs")
            .setCompression("zstd")
            .build()) {
      assertThat(
              exporter
                  .export(Collections.singletonList(generateFakeLogRecordData()))
                  .join(10, TimeUnit.SECONDS)
                  .isSuccess())
          .isTrue();

      assertThat(httpRequests)
          .satisfiesExactly(
              req -> assertThat(req.headers().contains("content-encoding", "zstd")).isTrue());
      assertThat(exportedTelemetry)
          .satisfiesExactly(
              resourceLogs ->
                  assertThat(resourceLogs.getScopeLogsList())
                      .satisfiesExactly(
                          scopeLogs -> {
                            InstrumentationScope scope = scopeLogs.getScope();
                            assertThat(scope.getName()).isEqualTo("testLib");
                            assertThat(scope.getVersion()).isEqualTo("1.0");
                            assertThat(scopeLogs.getSchemaUrl()).isEqualTo("http://url");
                            assertThat(scopeLogs.getLogRecordsList())
                                .satisfiesExactly(
                                    logRecord -> {
                                      assertThat(logRecord.getBody().getStringValue())
                                          .isEqualTo("log body");
                                      assertThat(logRecord.getAttributesList())
                                          .isEqualTo(
                                              Collections.singletonList(
                                                  KeyValue.newBuilder()
                                                      .setKey("key")
                                                      .setValue(
                                                          AnyValue.newBuilder()
                                                              .setStringValue("value")
                                                              .build())
                                                      .build()));
                                      assertThat(logRecord.getSeverityText()).isEqualTo("INFO");
                                      assertThat(logRecord.getSeverityNumber())
                                          .isEqualTo(SeverityNumber.SEVERITY_NUMBER_INFO);
                                      assertThat(logRecord.getTimeUnixNano()).isGreaterThan(0);
                                      assertThat(logRecord.getObservedTimeUnixNano())
                                          .isGreaterThan(0);
                                    });
                          }));
    }
  }

  /** Generate a fake {@link LogRecordData}. */
  public static LogRecordData generateFakeLogRecordData() {
    return TestLogRecordData.builder()
        .setResource(Resource.getDefault())
        .setInstrumentationScopeInfo(
            InstrumentationScopeInfo.builder("testLib")
                .setVersion("1.0")
                .setSchemaUrl("http://url")
                .build())
        .setBody("log body")
        .setAttributes(Attributes.builder().put("key", "value").build())
        .setSeverity(Severity.INFO)
        .setSeverityText(Severity.INFO.name())
        .setTimestamp(Instant.now())
        .setObservedTimestamp(Instant.now().plusNanos(100))
        .build();
  }
}
