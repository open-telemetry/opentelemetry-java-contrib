/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering;

import static io.opentelemetry.contrib.disk.buffering.internal.storage.TestData.MIN_FILE_AGE_FOR_READ_MILLIS;
import static java.util.Collections.singletonList;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import io.opentelemetry.contrib.disk.buffering.internal.exporter.FromDiskExporterImpl;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.serializers.SignalSerializer;
import io.opentelemetry.contrib.disk.buffering.internal.storage.TestData;
import io.opentelemetry.sdk.common.Clock;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@SuppressWarnings("unchecked")
class FromDiskExporterImplTest {
  private SpanExporter wrapped;
  private SignalSerializer<SpanData> serializer;
  private Clock clock;
  private FromDiskExporterImpl<SpanData> exporter;
  private final List<SpanData> deserializedData = Collections.emptyList();
  @TempDir File rootDir;
  private static final String STORAGE_FOLDER_NAME = "testName";

  @BeforeEach
  void setUp() throws IOException {
    clock = createClockMock();

    setUpSerializer();
    wrapped = mock();
    exporter =
        FromDiskExporterImpl.<SpanData>builder()
            .setFolderName(STORAGE_FOLDER_NAME)
            .setStorageConfiguration(TestData.getDefaultConfiguration(rootDir))
            .setDeserializer(serializer)
            .setExportFunction(wrapped::export)
            .setStorageClock(clock)
            .build();
  }

  @Test
  void whenExportingStoredBatch_withAvailableData_andSuccessfullyProcessed_returnTrue()
      throws IOException {
    doReturn(CompletableResultCode.ofSuccess()).when(wrapped).export(deserializedData);

    createDummyFile();
    doReturn(MILLISECONDS.toNanos(1000L + MIN_FILE_AGE_FOR_READ_MILLIS)).when(clock).now();

    assertThat(exporter.exportStoredBatch(1, TimeUnit.SECONDS)).isTrue();
  }

  @Test
  void whenExportingStoredBatch_withAvailableData_andUnsuccessfullyProcessed_returnFalse()
      throws IOException {
    doReturn(CompletableResultCode.ofFailure()).when(wrapped).export(deserializedData);

    createDummyFile();
    doReturn(1000L + MIN_FILE_AGE_FOR_READ_MILLIS).when(clock).now();

    assertThat(exporter.exportStoredBatch(1, TimeUnit.SECONDS)).isFalse();
  }

  @Test
  void whenExportingStoredBatch_withNoAvailableData_returnFalse() throws IOException {
    assertThat(exporter.exportStoredBatch(1, TimeUnit.SECONDS)).isFalse();
  }

  @Test
  void verifyStorageFolderIsCreated() {
    assertThat(new File(rootDir, STORAGE_FOLDER_NAME).exists()).isTrue();
  }

  private void createDummyFile() throws IOException {
    File file = new File(rootDir, STORAGE_FOLDER_NAME + "/" + 1000L);
    Files.write(file.toPath(), singletonList("First line"));
  }

  private void setUpSerializer() {
    serializer = mock();
    doReturn(deserializedData).when(serializer).deserialize(any());
  }

  private static Clock createClockMock() {
    Clock mock = mock();
    doReturn(MILLISECONDS.toNanos(1000L)).when(mock).now();
    return mock;
  }
}
