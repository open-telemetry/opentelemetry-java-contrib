/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.logs;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.api.common.Value;
import io.opentelemetry.api.logs.Severity;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.logs.models.LogRecordDataImpl;
import io.opentelemetry.contrib.disk.buffering.testutils.TestData;
import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceRequest;
import io.opentelemetry.proto.logs.v1.LogRecord;
import io.opentelemetry.proto.logs.v1.ResourceLogs;
import io.opentelemetry.proto.logs.v1.ScopeLogs;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProtoLogsDataMapperTest {

  private static final LogRecordData LOG_RECORD =
      LogRecordDataImpl.builder()
          .setResource(TestData.RESOURCE_FULL)
          .setSpanContext(TestData.SPAN_CONTEXT)
          .setInstrumentationScopeInfo(TestData.INSTRUMENTATION_SCOPE_INFO_FULL)
          .setAttributes(TestData.ATTRIBUTES)
          .setBodyValue(Value.of("Log body"))
          .setSeverity(Severity.DEBUG)
          .setSeverityText("Log severity text")
          .setTimestampEpochNanos(100L)
          .setObservedTimestampEpochNanos(200L)
          .setTotalAttributeCount(3)
          .setEventName("")
          .build();

  private static final LogRecordData OTHER_LOG_RECORD =
      LogRecordDataImpl.builder()
          .setResource(TestData.RESOURCE_FULL)
          .setSpanContext(TestData.SPAN_CONTEXT)
          .setInstrumentationScopeInfo(TestData.INSTRUMENTATION_SCOPE_INFO_FULL)
          .setAttributes(TestData.ATTRIBUTES)
          .setBodyValue(Value.of("Other log body"))
          .setSeverity(Severity.DEBUG)
          .setSeverityText("Log severity text")
          .setTimestampEpochNanos(100L)
          .setObservedTimestampEpochNanos(200L)
          .setTotalAttributeCount(3)
          .setEventName("")
          .build();

  private static final LogRecordData LOG_RECORD_WITH_DIFFERENT_SCOPE_SAME_RESOURCE =
      LogRecordDataImpl.builder()
          .setResource(TestData.RESOURCE_FULL)
          .setSpanContext(TestData.SPAN_CONTEXT)
          .setInstrumentationScopeInfo(TestData.INSTRUMENTATION_SCOPE_INFO_WITHOUT_VERSION)
          .setAttributes(TestData.ATTRIBUTES)
          .setBodyValue(Value.of("Same resource other scope log"))
          .setSeverity(Severity.DEBUG)
          .setSeverityText("Log severity text")
          .setTimestampEpochNanos(100L)
          .setObservedTimestampEpochNanos(200L)
          .setTotalAttributeCount(3)
          .setEventName("")
          .build();

  private static final LogRecordData LOG_RECORD_WITH_DIFFERENT_RESOURCE =
      LogRecordDataImpl.builder()
          .setResource(TestData.RESOURCE_WITHOUT_SCHEMA_URL)
          .setSpanContext(TestData.SPAN_CONTEXT)
          .setInstrumentationScopeInfo(TestData.INSTRUMENTATION_SCOPE_INFO_WITHOUT_VERSION)
          .setAttributes(TestData.ATTRIBUTES)
          .setBodyValue(Value.of("Different resource log"))
          .setSeverity(Severity.DEBUG)
          .setSeverityText("Log severity text")
          .setTimestampEpochNanos(100L)
          .setObservedTimestampEpochNanos(200L)
          .setTotalAttributeCount(3)
          .setEventName("")
          .build();

  private static final LogRecordData LOG_RECORD_WITH_EVENT_NAME =
      LogRecordDataImpl.builder()
          .setResource(TestData.RESOURCE_FULL)
          .setSpanContext(TestData.SPAN_CONTEXT)
          .setInstrumentationScopeInfo(TestData.INSTRUMENTATION_SCOPE_INFO_FULL)
          .setAttributes(TestData.ATTRIBUTES)
          .setBodyValue(Value.of("Log body"))
          .setSeverity(Severity.DEBUG)
          .setSeverityText("Log severity text")
          .setTimestampEpochNanos(100L)
          .setObservedTimestampEpochNanos(200L)
          .setTotalAttributeCount(3)
          .setEventName("test.event.name")
          .build();

  @Test
  void verifyConversionDataStructure() {
    List<LogRecordData> signals = Collections.singletonList(LOG_RECORD);

    ExportLogsServiceRequest result = mapToProto(signals);

    List<ResourceLogs> resourceLogsList = result.resource_logs;
    assertThat(resourceLogsList).hasSize(1);
    assertThat(resourceLogsList.get(0).scope_logs).hasSize(1);
    assertThat(resourceLogsList.get(0).scope_logs.get(0).log_records).hasSize(1);

    assertThat(mapFromProto(result)).containsExactlyInAnyOrderElementsOf(signals);
  }

  @Test
  void verifyMultipleLogsWithSameResourceAndScope() {
    List<LogRecordData> signals = Arrays.asList(LOG_RECORD, OTHER_LOG_RECORD);

    ExportLogsServiceRequest proto = mapToProto(signals);

    List<ResourceLogs> resourceLogsList = proto.resource_logs;
    assertThat(resourceLogsList).hasSize(1);
    List<ScopeLogs> scopeLogsList = resourceLogsList.get(0).scope_logs;
    assertThat(scopeLogsList).hasSize(1);
    List<LogRecord> logRecords = scopeLogsList.get(0).log_records;
    assertThat(logRecords).hasSize(2);
    assertThat(logRecords.get(0).body.string_value).isEqualTo("Log body");
    assertThat(logRecords.get(1).body.string_value).isEqualTo("Other log body");

    assertThat(mapFromProto(proto)).hasSize(2);

    assertThat(mapFromProto(proto)).containsExactlyInAnyOrderElementsOf(signals);
  }

  @Test
  void verifyMultipleLogsWithSameResourceDifferentScope() {
    List<LogRecordData> signals =
        Arrays.asList(LOG_RECORD, LOG_RECORD_WITH_DIFFERENT_SCOPE_SAME_RESOURCE);

    ExportLogsServiceRequest proto = mapToProto(signals);

    List<ResourceLogs> resourceLogsList = proto.resource_logs;
    assertThat(resourceLogsList).hasSize(1);
    List<ScopeLogs> scopeLogsList = resourceLogsList.get(0).scope_logs;
    assertThat(scopeLogsList).hasSize(2);
    ScopeLogs firstScope = scopeLogsList.get(0);
    ScopeLogs secondScope = scopeLogsList.get(1);
    List<LogRecord> firstScopeLogs = firstScope.log_records;
    List<LogRecord> secondScopeLogs = secondScope.log_records;
    assertThat(firstScopeLogs).hasSize(1);
    assertThat(secondScopeLogs).hasSize(1);

    assertThat(mapFromProto(proto)).containsExactlyInAnyOrderElementsOf(signals);
  }

  @Test
  void verifyMultipleLogsWithDifferentResource() {
    List<LogRecordData> signals = Arrays.asList(LOG_RECORD, LOG_RECORD_WITH_DIFFERENT_RESOURCE);

    ExportLogsServiceRequest proto = mapToProto(signals);

    List<ResourceLogs> resourceLogsList = proto.resource_logs;
    assertThat(resourceLogsList).hasSize(2);
    ResourceLogs firstResourceLogs = resourceLogsList.get(0);
    ResourceLogs secondResourceLogs = resourceLogsList.get(1);
    List<ScopeLogs> firstScopeLogsList = firstResourceLogs.scope_logs;
    List<ScopeLogs> secondScopeLogsList = secondResourceLogs.scope_logs;
    assertThat(firstScopeLogsList).hasSize(1);
    assertThat(secondScopeLogsList).hasSize(1);
    ScopeLogs firstScope = firstScopeLogsList.get(0);
    ScopeLogs secondScope = secondScopeLogsList.get(0);
    List<LogRecord> firstScopeLogs = firstScope.log_records;
    List<LogRecord> secondScopeLogs = secondScope.log_records;
    assertThat(firstScopeLogs).hasSize(1);
    assertThat(secondScopeLogs).hasSize(1);

    assertThat(mapFromProto(proto)).containsExactlyInAnyOrderElementsOf(signals);
  }

  @Test
  void verifyLogWithEventName() {
    List<LogRecordData> signals = Collections.singletonList(LOG_RECORD_WITH_EVENT_NAME);

    ExportLogsServiceRequest result = mapToProto(signals);

    List<ResourceLogs> resourceLogsList = result.resource_logs;
    LogRecord firstLog = resourceLogsList.get(0).scope_logs.get(0).log_records.get(0);

    assertThat(firstLog.event_name).isEqualTo("test.event.name");
    assertThat(mapFromProto(result)).containsExactlyInAnyOrderElementsOf(signals);
  }

  private static ExportLogsServiceRequest mapToProto(Collection<LogRecordData> signals) {
    return ProtoLogsDataMapper.getInstance().toProto(signals);
  }

  private static List<LogRecordData> mapFromProto(ExportLogsServiceRequest protoData) {
    return ProtoLogsDataMapper.getInstance().fromProto(protoData);
  }
}
