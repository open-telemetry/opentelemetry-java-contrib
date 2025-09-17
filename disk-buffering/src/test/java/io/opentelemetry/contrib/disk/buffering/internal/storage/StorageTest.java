/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.storage;

import static io.opentelemetry.contrib.disk.buffering.internal.storage.responses.ReadableResult.TRY_LATER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.opentelemetry.contrib.disk.buffering.config.StorageConfiguration;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.serializers.ByteArraySerializer;
import io.opentelemetry.contrib.disk.buffering.internal.storage.files.ReadableFile;
import io.opentelemetry.contrib.disk.buffering.internal.storage.files.WritableFile;
import io.opentelemetry.contrib.disk.buffering.internal.storage.files.reader.ProcessResult;
import io.opentelemetry.contrib.disk.buffering.internal.storage.responses.ReadableResult;
import io.opentelemetry.contrib.disk.buffering.internal.storage.responses.WritableResult;
import io.opentelemetry.contrib.disk.buffering.internal.utils.SignalTypes;
import java.io.File;
import java.io.IOException;
import java.util.function.Function;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@SuppressWarnings("unchecked")
class StorageTest {
  private FolderManager folderManager;
  private Storage storage;
  private Function<byte[], ProcessResult> processing;
  private ReadableFile readableFile;
  private WritableFile writableFile;

  @BeforeEach
  void setUp() throws IOException {
    folderManager = mock();
    readableFile = mock();
    writableFile = createWritableFile();
    processing = mock();
    when(readableFile.readAndProcess(processing)).thenReturn(ReadableResult.SUCCEEDED);
    storage = new Storage(folderManager, true);
  }

  @AfterEach
  void tearDown() throws IOException {
    storage.close();
  }

  @Test
  void whenReadingAndProcessingSuccessfully_returnSuccess() throws IOException {
    when(folderManager.getReadableFile()).thenReturn(readableFile);

    assertThat(storage.readAndProcess(processing)).isEqualTo(ReadableResult.SUCCEEDED);

    verify(readableFile).readAndProcess(processing);
  }

  @Test
  void whenReadableFileProcessingFails_returnTryLater() throws IOException {
    when(folderManager.getReadableFile()).thenReturn(readableFile);
    when(readableFile.readAndProcess(processing)).thenReturn(TRY_LATER);

    assertThat(storage.readAndProcess(processing)).isEqualTo(TRY_LATER);

    verify(readableFile).readAndProcess(processing);
  }

  @Test
  void whenReadingMultipleTimes_reuseReader() throws IOException {
    ReadableFile anotherReadable = mock();
    when(folderManager.getReadableFile()).thenReturn(readableFile).thenReturn(anotherReadable);

    assertThat(storage.readAndProcess(processing)).isEqualTo(ReadableResult.SUCCEEDED);
    assertThat(storage.readAndProcess(processing)).isEqualTo(ReadableResult.SUCCEEDED);

    verify(readableFile, times(2)).readAndProcess(processing);
    verify(folderManager, times(1)).getReadableFile();
    verifyNoInteractions(anotherReadable);
  }

  @Test
  void whenWritingMultipleTimes_reuseWriter() throws IOException {
    ByteArraySerializer data = new ByteArraySerializer(new byte[1]);
    WritableFile anotherWriter = createWritableFile();
    when(folderManager.createWritableFile()).thenReturn(writableFile).thenReturn(anotherWriter);

    storage.write(data);
    storage.write(data);

    verify(writableFile, times(2)).append(data);
    verify(folderManager, times(1)).createWritableFile();
    verifyNoInteractions(anotherWriter);
  }

  @Test
  void whenAttemptingToReadAfterClosed_returnFailed() throws IOException {
    storage.close();
    assertThat(storage.readAndProcess(processing)).isEqualTo(ReadableResult.FAILED);
  }

  @Test
  void whenAttemptingToWriteAfterClosed_returnFalse() throws IOException {
    storage.close();
    assertThat(storage.write(new ByteArraySerializer(new byte[1]))).isFalse();
  }

  @Test
  void whenNoFileAvailableForReading_returnFailed() throws IOException {
    assertThat(storage.readAndProcess(processing)).isEqualTo(ReadableResult.FAILED);
  }

  @Test
  void whenTheReadTimeExpires_lookForNewFileToRead() throws IOException {
    when(folderManager.getReadableFile()).thenReturn(readableFile).thenReturn(null);
    when(readableFile.readAndProcess(processing)).thenReturn(ReadableResult.FAILED);

    storage.readAndProcess(processing);

    verify(folderManager, times(2)).getReadableFile();
  }

  @Test
  void whenNoMoreLinesToRead_lookForNewFileToRead() throws IOException {
    when(folderManager.getReadableFile()).thenReturn(readableFile).thenReturn(null);
    when(readableFile.readAndProcess(processing)).thenReturn(ReadableResult.FAILED);

    storage.readAndProcess(processing);

    verify(folderManager, times(2)).getReadableFile();
  }

  @Test
  void whenResourceClosed_lookForNewFileToRead() throws IOException {
    when(folderManager.getReadableFile()).thenReturn(readableFile).thenReturn(null);
    when(readableFile.readAndProcess(processing)).thenReturn(ReadableResult.FAILED);

    storage.readAndProcess(processing);

    verify(folderManager, times(2)).getReadableFile();
  }

  @Test
  void whenEveryNewFileFoundCannotBeRead_returnContentNotAvailable() throws IOException {
    when(folderManager.getReadableFile()).thenReturn(readableFile);
    when(readableFile.readAndProcess(processing)).thenReturn(ReadableResult.FAILED);

    assertThat(storage.readAndProcess(processing)).isEqualTo(ReadableResult.FAILED);

    verify(folderManager, times(3)).getReadableFile();
  }

  @Test
  void appendDataToFile() throws IOException {
    when(folderManager.createWritableFile()).thenReturn(writableFile);
    ByteArraySerializer data = new ByteArraySerializer(new byte[1]);

    storage.write(data);

    verify(writableFile).append(data);
  }

  @Test
  void whenWritingTimeoutHappens_retryWithNewFile() throws IOException {
    ByteArraySerializer data = new ByteArraySerializer(new byte[1]);
    WritableFile workingWritableFile = createWritableFile();
    when(folderManager.createWritableFile())
        .thenReturn(writableFile)
        .thenReturn(workingWritableFile);
    when(writableFile.append(data)).thenReturn(WritableResult.FAILED);

    storage.write(data);

    verify(folderManager, times(2)).createWritableFile();
  }

  @Test
  void whenThereIsNoSpaceAvailableForWriting_retryWithNewFile() throws IOException {
    ByteArraySerializer data = new ByteArraySerializer(new byte[1]);
    WritableFile workingWritableFile = createWritableFile();
    when(folderManager.createWritableFile())
        .thenReturn(writableFile)
        .thenReturn(workingWritableFile);
    when(writableFile.append(data)).thenReturn(WritableResult.FAILED);

    storage.write(data);

    verify(folderManager, times(2)).createWritableFile();
  }

  @Test
  void whenWritingResourceIsClosed_retryWithNewFile() throws IOException {
    ByteArraySerializer data = new ByteArraySerializer(new byte[1]);
    WritableFile workingWritableFile = createWritableFile();
    when(folderManager.createWritableFile())
        .thenReturn(writableFile)
        .thenReturn(workingWritableFile);
    when(writableFile.append(data)).thenReturn(WritableResult.FAILED);

    storage.write(data);

    verify(folderManager, times(2)).createWritableFile();
  }

  @Test
  void whenEveryAttemptToWriteFails_returnFalse() throws IOException {
    ByteArraySerializer data = new ByteArraySerializer(new byte[1]);
    when(folderManager.createWritableFile()).thenReturn(writableFile);
    when(writableFile.append(data)).thenReturn(WritableResult.FAILED);

    assertThat(storage.write(data)).isFalse();

    verify(folderManager, times(3)).createWritableFile();
  }

  @Test
  void whenClosing_closeWriterAndReaderIfNotNull() throws IOException {
    when(folderManager.createWritableFile()).thenReturn(writableFile);
    when(folderManager.getReadableFile()).thenReturn(readableFile);
    storage.write(new ByteArraySerializer(new byte[1]));
    storage.readAndProcess(processing);

    storage.close();

    verify(writableFile).close();
    verify(readableFile).close();
  }

  @Test
  void whenMinFileReadIsNotGraterThanMaxFileWrite_throwException() {
    StorageConfiguration invalidConfig =
        StorageConfiguration.builder()
            .setMaxFileAgeForWriteMillis(2)
            .setMinFileAgeForReadMillis(1)
            .setRootDir(new File("."))
            .build();

    assertThatThrownBy(
            () -> Storage.builder(SignalTypes.logs).setStorageConfiguration(invalidConfig))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(
            "The configured max file age for writing must be lower than the configured min file age for reading");
  }

  private static WritableFile createWritableFile() throws IOException {
    WritableFile mock = mock();
    when(mock.append(any())).thenReturn(WritableResult.SUCCEEDED);
    return mock;
  }
}
