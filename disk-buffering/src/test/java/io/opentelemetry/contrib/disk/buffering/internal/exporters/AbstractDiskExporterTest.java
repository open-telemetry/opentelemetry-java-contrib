/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.exporters;

import static io.opentelemetry.contrib.disk.buffering.internal.storage.TestData.MIN_FILE_AGE_FOR_READ_MILLIS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import io.opentelemetry.contrib.disk.buffering.internal.serialization.serializers.SignalSerializer;
import io.opentelemetry.contrib.disk.buffering.internal.storage.TestData;
import io.opentelemetry.contrib.disk.buffering.internal.storage.exceptions.ResourceClosedException;
import io.opentelemetry.contrib.disk.buffering.internal.storage.utils.TimeProvider;
import io.opentelemetry.contrib.disk.buffering.storage.StorageConfiguration;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AbstractDiskExporterTest {
  private SpanExporter wrapped;
  private SignalSerializer<SpanData> serializer;
  private TimeProvider timeProvider;
  private AbstractDiskExporter<SpanData> exporter;
  private final List<SpanData> deserializedData = Collections.emptyList();
  @TempDir File rootDir;
  private static final String STORAGE_FOLDER_NAME = "testName";

  @BeforeEach
  public void setUp() throws NoSuchFieldException, IllegalAccessException {
    setUpTimeProvider();
    setUpSerializer();
    wrapped = mock();
    exporter = new TestDiskExporter(wrapped, rootDir, TestData.CONFIGURATION, serializer);
  }

  @Test
  public void whenExportingStoredBatch_withAvailableData_andSuccessfullyProcessed_returnTrue()
      throws IOException {
    doReturn(CompletableResultCode.ofSuccess()).when(wrapped).export(deserializedData);

    createDummyFile(1000L, "First line");
    doReturn(1000L + MIN_FILE_AGE_FOR_READ_MILLIS).when(timeProvider).getSystemCurrentTimeMillis();

    assertTrue(exporter.exportStoredBatch(1, TimeUnit.SECONDS));
  }

  @Test
  public void whenMinFileReadIsNotGraterThanMaxFileWrite_throwException() {
    try {
      new TestDiskExporter(
          wrapped,
          rootDir,
          StorageConfiguration.builder()
              .setMaxFileAgeForWriteMillis(2)
              .setMinFileAgeForReadMillis(1)
              .build(),
          serializer);
      fail();
    } catch (IllegalArgumentException e) {
      assertEquals(
          "The configured max file age for writing must be lower than the configured min file age for reading",
          e.getMessage());
    }
  }

  @Test
  public void whenExportingStoredBatch_withAvailableData_andUnsuccessfullyProcessed_returnFalse()
      throws IOException {
    doReturn(CompletableResultCode.ofFailure()).when(wrapped).export(deserializedData);

    createDummyFile(1000L, "First line");
    doReturn(1000L + MIN_FILE_AGE_FOR_READ_MILLIS).when(timeProvider).getSystemCurrentTimeMillis();

    assertFalse(exporter.exportStoredBatch(1, TimeUnit.SECONDS));
  }

  @Test
  public void whenExportingStoredBatch_withNoAvailableData_returnFalse() throws IOException {
    assertFalse(exporter.exportStoredBatch(1, TimeUnit.SECONDS));
  }

  @Test
  public void verifyStorageFolderIsCreated() {
    assertTrue(new File(rootDir, STORAGE_FOLDER_NAME).exists());
  }

  @Test
  public void closeStorageOnShutDown() throws IOException {
    exporter.onShutDown();

    try {
      exporter.exportStoredBatch(1, TimeUnit.SECONDS);
      fail();
    } catch (ResourceClosedException ignored) {
    }
  }

  @Test
  public void whenWritingSucceedsOnExport_returnSuccessfulResultCode() {
    doReturn(new byte[2]).when(serializer).serialize(deserializedData);

    CompletableResultCode completableResultCode = exporter.onExport(deserializedData);

    assertTrue(completableResultCode.isSuccess());
    verifyNoInteractions(wrapped);
  }

  @Test
  public void whenWritingFailsOnExport_doExportRightAway() throws IOException {
    doReturn(CompletableResultCode.ofSuccess()).when(wrapped).export(deserializedData);
    exporter.onShutDown();

    CompletableResultCode completableResultCode = exporter.onExport(deserializedData);

    assertTrue(completableResultCode.isSuccess());
    verify(wrapped).export(deserializedData);
  }

  private File createDummyFile(long createdTimeMillis, String... lines) throws IOException {
    File file = new File(rootDir, exporter.getStorageFolderName() + "/" + createdTimeMillis);
    Files.write(file.toPath(), Arrays.asList(lines));
    return file;
  }

  private void setUpTimeProvider() throws NoSuchFieldException, IllegalAccessException {
    timeProvider = mock();
    Field field = TimeProvider.class.getDeclaredField("instance");
    field.setAccessible(true);

    field.set(null, timeProvider);
    doReturn(1000L).when(timeProvider).getSystemCurrentTimeMillis();
  }

  private void setUpSerializer() {
    serializer = mock();
    doReturn(deserializedData).when(serializer).deserialize(any());
  }

  private static class TestDiskExporter extends AbstractDiskExporter<SpanData> {
    private final SpanExporter wrapped;
    private final SignalSerializer<SpanData> serializer;

    public TestDiskExporter(
        SpanExporter wrapped,
        File rootDir,
        StorageConfiguration configuration,
        SignalSerializer<SpanData> serializer) {
      super(rootDir, configuration);
      this.wrapped = wrapped;
      this.serializer = serializer;
    }

    @Override
    protected String getStorageFolderName() {
      return STORAGE_FOLDER_NAME;
    }

    @Override
    protected CompletableResultCode doExport(Collection<SpanData> spanData) {
      return wrapped.export(spanData);
    }

    @Override
    protected SignalSerializer<SpanData> getSerializer() {
      return serializer;
    }
  }
}
