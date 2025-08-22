/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.storage;

import io.opentelemetry.api.common.Value;
import io.opentelemetry.api.logs.Severity;
import io.opentelemetry.contrib.disk.buffering.config.StorageConfiguration;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.logs.models.LogRecordDataImpl;
import io.opentelemetry.sdk.logs.data.LogRecordData;

public final class TestData {

  public static final LogRecordData FIRST_LOG_RECORD =
      LogRecordDataImpl.builder()
          .setResource(io.opentelemetry.contrib.disk.buffering.testutils.TestData.RESOURCE_FULL)
          .setSpanContext(io.opentelemetry.contrib.disk.buffering.testutils.TestData.SPAN_CONTEXT)
          .setInstrumentationScopeInfo(
              io.opentelemetry.contrib.disk.buffering.testutils.TestData
                  .INSTRUMENTATION_SCOPE_INFO_FULL)
          .setAttributes(io.opentelemetry.contrib.disk.buffering.testutils.TestData.ATTRIBUTES)
          .setBodyValue(Value.of("First log body"))
          .setSeverity(Severity.DEBUG)
          .setSeverityText("Log severity text")
          .setTimestampEpochNanos(100L)
          .setObservedTimestampEpochNanos(200L)
          .setTotalAttributeCount(3)
          .setEventName("")
          .build();

  public static final LogRecordData SECOND_LOG_RECORD =
      LogRecordDataImpl.builder()
          .setResource(io.opentelemetry.contrib.disk.buffering.testutils.TestData.RESOURCE_FULL)
          .setSpanContext(io.opentelemetry.contrib.disk.buffering.testutils.TestData.SPAN_CONTEXT)
          .setInstrumentationScopeInfo(
              io.opentelemetry.contrib.disk.buffering.testutils.TestData
                  .INSTRUMENTATION_SCOPE_INFO_FULL)
          .setAttributes(io.opentelemetry.contrib.disk.buffering.testutils.TestData.ATTRIBUTES)
          .setBodyValue(Value.of("Second log body"))
          .setSeverity(Severity.DEBUG)
          .setSeverityText("Log severity text")
          .setTimestampEpochNanos(100L)
          .setObservedTimestampEpochNanos(200L)
          .setTotalAttributeCount(3)
          .setEventName("event")
          .build();

  public static final LogRecordData THIRD_LOG_RECORD =
      LogRecordDataImpl.builder()
          .setResource(io.opentelemetry.contrib.disk.buffering.testutils.TestData.RESOURCE_FULL)
          .setSpanContext(io.opentelemetry.contrib.disk.buffering.testutils.TestData.SPAN_CONTEXT)
          .setInstrumentationScopeInfo(
              io.opentelemetry.contrib.disk.buffering.testutils.TestData
                  .INSTRUMENTATION_SCOPE_INFO_FULL)
          .setAttributes(io.opentelemetry.contrib.disk.buffering.testutils.TestData.ATTRIBUTES)
          .setBodyValue(Value.of("Third log body"))
          .setSeverity(Severity.DEBUG)
          .setSeverityText("Log severity text")
          .setTimestampEpochNanos(100L)
          .setObservedTimestampEpochNanos(200L)
          .setTotalAttributeCount(3)
          .setEventName("")
          .build();

  public static final long MAX_FILE_AGE_FOR_WRITE_MILLIS = 1000;
  public static final long MIN_FILE_AGE_FOR_READ_MILLIS = MAX_FILE_AGE_FOR_WRITE_MILLIS + 500;
  public static final long MAX_FILE_AGE_FOR_READ_MILLIS = 10_000;
  public static final int MAX_FILE_SIZE = 2000;
  public static final int MAX_FOLDER_SIZE = 6000;

  public static StorageConfiguration getConfiguration() {
    return StorageConfiguration.builder()
        .setMaxFileAgeForWriteMillis(MAX_FILE_AGE_FOR_WRITE_MILLIS)
        .setMinFileAgeForReadMillis(MIN_FILE_AGE_FOR_READ_MILLIS)
        .setMaxFileAgeForReadMillis(MAX_FILE_AGE_FOR_READ_MILLIS)
        .setMaxFileSize(MAX_FILE_SIZE)
        .setMaxFolderSize(MAX_FOLDER_SIZE)
        .build();
  }

  private TestData() {}
}
