/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.opentelemetry.contrib.disk.buffering.internal.storage.files.ReadableFile;
import io.opentelemetry.contrib.disk.buffering.internal.storage.files.WritableFile;
import io.opentelemetry.contrib.disk.buffering.internal.storage.responses.ReadableResult;
import io.opentelemetry.contrib.disk.buffering.internal.storage.responses.WritableResult;
import java.io.IOException;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@SuppressWarnings("unchecked")
class StorageTest {
  private FolderManager folderManager;
  private Storage storage;
  private Function<byte[], Boolean> processing;
  private ReadableFile readableFile;
  private WritableFile writableFile;

  @BeforeEach
  public void setUp() throws IOException {
    folderManager = mock();
    readableFile = mock();
    writableFile = createWritableFile();
    processing = mock();
    doReturn(ReadableResult.SUCCEEDED).when(readableFile).readAndProcess(processing);
    storage = new Storage(folderManager);
  }

  @Test
  public void whenReadingAndProcessingSuccessfully_returnSuccess() throws IOException {
    doReturn(readableFile).when(folderManager).getReadableFile();

    assertEquals(ReadableResult.SUCCEEDED, storage.readAndProcess(processing));

    verify(readableFile).readAndProcess(processing);
  }

  @Test
  public void whenReadableFileProcessingFails_returnFailed() throws IOException {
    doReturn(readableFile).when(folderManager).getReadableFile();
    doReturn(ReadableResult.PROCESSING_FAILED).when(readableFile).readAndProcess(processing);

    assertEquals(ReadableResult.PROCESSING_FAILED, storage.readAndProcess(processing));

    verify(readableFile).readAndProcess(processing);
  }

  @Test
  public void whenReadingMultipleTimes_reuseReader() throws IOException {
    ReadableFile anotherReadable = mock();
    when(folderManager.getReadableFile()).thenReturn(readableFile).thenReturn(anotherReadable);

    assertEquals(ReadableResult.SUCCEEDED, storage.readAndProcess(processing));
    assertEquals(ReadableResult.SUCCEEDED, storage.readAndProcess(processing));

    verify(readableFile, times(2)).readAndProcess(processing);
    verify(folderManager, times(1)).getReadableFile();
    verifyNoInteractions(anotherReadable);
  }

  @Test
  public void whenWritingMultipleTimes_reuseWriter() throws IOException {
    byte[] data = new byte[1];
    WritableFile anotherWriter = createWritableFile();
    when(folderManager.createWritableFile()).thenReturn(writableFile).thenReturn(anotherWriter);

    storage.write(data);
    storage.write(data);

    verify(writableFile, times(2)).append(data);
    verify(folderManager, times(1)).createWritableFile();
    verifyNoInteractions(anotherWriter);
  }

  @Test
  public void whenAttemptingToReadAfterClosed_returnClosed() throws IOException {
    storage.close();
    assertEquals(ReadableResult.CLOSED, storage.readAndProcess(processing));
  }

  @Test
  public void whenAttemptingToWriteAfterClosed_returnFalse() throws IOException {
    storage.close();
    assertFalse(storage.write(new byte[1]));
  }

  @Test
  public void whenNoFileAvailableForReading_returnNoContentAvailable() throws IOException {
    assertEquals(ReadableResult.NO_CONTENT_AVAILABLE, storage.readAndProcess(processing));
  }

  @Test
  public void whenTheReadTimeExpires_lookForNewFileToRead() throws IOException {
    when(folderManager.getReadableFile()).thenReturn(readableFile).thenReturn(null);
    doReturn(ReadableResult.FILE_HAS_EXPIRED).when(readableFile).readAndProcess(processing);

    storage.readAndProcess(processing);

    verify(folderManager, times(2)).getReadableFile();
  }

  @Test
  public void whenNoMoreLinesToRead_lookForNewFileToRead() throws IOException {
    when(folderManager.getReadableFile()).thenReturn(readableFile).thenReturn(null);
    doReturn(ReadableResult.NO_CONTENT_AVAILABLE).when(readableFile).readAndProcess(processing);

    storage.readAndProcess(processing);

    verify(folderManager, times(2)).getReadableFile();
  }

  @Test
  public void whenResourceClosed_lookForNewFileToRead() throws IOException {
    when(folderManager.getReadableFile()).thenReturn(readableFile).thenReturn(null);
    doReturn(ReadableResult.CLOSED).when(readableFile).readAndProcess(processing);

    storage.readAndProcess(processing);

    verify(folderManager, times(2)).getReadableFile();
  }

  @Test
  public void whenEveryNewFileFoundCannotBeRead_returnContentNotAvailable() throws IOException {
    when(folderManager.getReadableFile()).thenReturn(readableFile);
    doReturn(ReadableResult.CLOSED).when(readableFile).readAndProcess(processing);

    assertEquals(ReadableResult.NO_CONTENT_AVAILABLE, storage.readAndProcess(processing));

    verify(folderManager, times(3)).getReadableFile();
  }

  @Test
  public void appendDataToFile() throws IOException {
    doReturn(writableFile).when(folderManager).createWritableFile();
    byte[] data = new byte[1];

    storage.write(data);

    verify(writableFile).append(data);
  }

  @Test
  public void whenWritingTimeoutHappens_retryWithNewFile() throws IOException {
    byte[] data = new byte[1];
    WritableFile workingWritableFile = createWritableFile();
    when(folderManager.createWritableFile())
        .thenReturn(writableFile)
        .thenReturn(workingWritableFile);
    doReturn(WritableResult.FILE_EXPIRED).when(writableFile).append(data);

    storage.write(data);

    verify(folderManager, times(2)).createWritableFile();
  }

  @Test
  public void whenThereIsNoSpaceAvailableForWriting_retryWithNewFile() throws IOException {
    byte[] data = new byte[1];
    WritableFile workingWritableFile = createWritableFile();
    when(folderManager.createWritableFile())
        .thenReturn(writableFile)
        .thenReturn(workingWritableFile);
    doReturn(WritableResult.FILE_IS_FULL).when(writableFile).append(data);

    storage.write(data);

    verify(folderManager, times(2)).createWritableFile();
  }

  @Test
  public void whenWritingResourceIsClosed_retryWithNewFile() throws IOException {
    byte[] data = new byte[1];
    WritableFile workingWritableFile = createWritableFile();
    when(folderManager.createWritableFile())
        .thenReturn(writableFile)
        .thenReturn(workingWritableFile);
    doReturn(WritableResult.CLOSED).when(writableFile).append(data);

    storage.write(data);

    verify(folderManager, times(2)).createWritableFile();
  }

  @Test
  public void whenEveryAttemptToWriteFails_returnFalse() throws IOException {
    byte[] data = new byte[1];
    when(folderManager.createWritableFile()).thenReturn(writableFile);
    doReturn(WritableResult.CLOSED).when(writableFile).append(data);

    assertFalse(storage.write(data));

    verify(folderManager, times(3)).createWritableFile();
  }

  @Test
  public void whenClosing_closeWriterAndReaderIfNotNull() throws IOException {
    doReturn(writableFile).when(folderManager).createWritableFile();
    doReturn(readableFile).when(folderManager).getReadableFile();
    storage.write(new byte[1]);
    storage.readAndProcess(processing);

    storage.close();

    verify(writableFile).close();
    verify(readableFile).close();
  }

  private static WritableFile createWritableFile() throws IOException {
    WritableFile mock = mock();
    doReturn(WritableResult.SUCCEEDED).when(mock).append(any());
    return mock;
  }
}
