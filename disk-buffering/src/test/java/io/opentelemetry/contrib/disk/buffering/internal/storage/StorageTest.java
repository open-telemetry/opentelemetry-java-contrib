package io.opentelemetry.contrib.disk.buffering.internal.storage;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.opentelemetry.contrib.disk.buffering.internal.storage.exceptions.MaxAttemptsReachedException;
import io.opentelemetry.contrib.disk.buffering.internal.storage.exceptions.NoMoreLinesToReadException;
import io.opentelemetry.contrib.disk.buffering.internal.storage.exceptions.NoSpaceAvailableException;
import io.opentelemetry.contrib.disk.buffering.internal.storage.exceptions.ReadingTimeoutException;
import io.opentelemetry.contrib.disk.buffering.internal.storage.exceptions.ResourceClosedException;
import io.opentelemetry.contrib.disk.buffering.internal.storage.exceptions.WritingTimeoutException;
import io.opentelemetry.contrib.disk.buffering.internal.storage.files.ReadableFile;
import io.opentelemetry.contrib.disk.buffering.internal.storage.files.WritableFile;
import java.io.IOException;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StorageTest {
  private FolderManager folderManager;
  private Storage storage;
  private Function<byte[], Boolean> consumer;
  private ReadableFile readableFile;
  private WritableFile writableFile;

  @BeforeEach
  public void setUp() {
    folderManager = mock();
    consumer = mock();
    readableFile = mock();
    writableFile = mock();
    storage = new Storage(folderManager);
  }

  @Test
  public void whenReadingSuccessfully_returnTrue() throws IOException {
    doReturn(readableFile).when(folderManager).getReadableFile();

    assertTrue(storage.read(consumer));

    verify(readableFile).readLine(consumer);
  }

  @Test
  public void whenAttemptingToReadAfterClosed_throwException() throws IOException {
    storage.close();
    try {
      storage.read(consumer);
      fail();
    } catch (ResourceClosedException ignored) {
    }
  }

  @Test
  public void whenAttemptingToWriteAfterClosed_throwException() throws IOException {
    storage.close();
    try {
      storage.write(new byte[1]);
      fail();
    } catch (ResourceClosedException ignored) {
    }
  }

  @Test
  public void whenNoFileAvailableForReading_returnFalse() throws IOException {
    assertFalse(storage.read(consumer));
  }

  @Test
  public void whenTimeoutExceptionHappens_lookForNewFileToRead() throws IOException {
    when(folderManager.getReadableFile()).thenReturn(readableFile).thenReturn(null);
    doThrow(ReadingTimeoutException.class).when(readableFile).readLine(consumer);

    assertFalse(storage.read(consumer));

    verify(folderManager, times(2)).getReadableFile();
  }

  @Test
  public void whenNoMoreLinesToReadExceptionHappens_lookForNewFileToRead() throws IOException {
    when(folderManager.getReadableFile()).thenReturn(readableFile).thenReturn(null);
    doThrow(NoMoreLinesToReadException.class).when(readableFile).readLine(consumer);

    assertFalse(storage.read(consumer));

    verify(folderManager, times(2)).getReadableFile();
  }

  @Test
  public void whenResourceClosedExceptionHappens_lookForNewFileToRead() throws IOException {
    when(folderManager.getReadableFile()).thenReturn(readableFile).thenReturn(null);
    doThrow(ResourceClosedException.class).when(readableFile).readLine(consumer);

    assertFalse(storage.read(consumer));

    verify(folderManager, times(2)).getReadableFile();
  }

  @Test
  public void whenEveryNewFileFoundCannotBeRead_throwExceptionAfterMaxAttempts()
      throws IOException {
    when(folderManager.getReadableFile()).thenReturn(readableFile);
    doThrow(ResourceClosedException.class).when(readableFile).readLine(consumer);

    try {
      assertFalse(storage.read(consumer));
      fail();
    } catch (MaxAttemptsReachedException e) {
      verify(folderManager, times(3)).getReadableFile();
    }
  }

  @Test
  public void appendLineToFile() throws IOException {
    doReturn(writableFile).when(folderManager).createWritableFile();
    byte[] data = new byte[1];

    storage.write(data);

    verify(writableFile).append(data);
  }

  @Test
  public void whenWritingTimeoutExceptionHappens_retryWithNewFile() throws IOException {
    byte[] data = new byte[1];
    WritableFile workingWritableFile = mock();
    when(folderManager.createWritableFile())
        .thenReturn(writableFile)
        .thenReturn(workingWritableFile);
    doThrow(WritingTimeoutException.class).when(writableFile).append(data);

    storage.write(data);

    verify(folderManager, times(2)).createWritableFile();
  }

  @Test
  public void whenNoSpaceAvailableExceptionHappens_retryWithNewFile() throws IOException {
    byte[] data = new byte[1];
    WritableFile workingWritableFile = mock();
    when(folderManager.createWritableFile())
        .thenReturn(writableFile)
        .thenReturn(workingWritableFile);
    doThrow(NoSpaceAvailableException.class).when(writableFile).append(data);

    storage.write(data);

    verify(folderManager, times(2)).createWritableFile();
  }

  @Test
  public void whenResourceClosedExceptionHappens_retryWithNewFile() throws IOException {
    byte[] data = new byte[1];
    WritableFile workingWritableFile = mock();
    when(folderManager.createWritableFile())
        .thenReturn(writableFile)
        .thenReturn(workingWritableFile);
    doThrow(ResourceClosedException.class).when(writableFile).append(data);

    storage.write(data);

    verify(folderManager, times(2)).createWritableFile();
  }

  @Test
  public void whenEveryAttemptToWriteFails_throwExceptionAfterMaxAttempts() throws IOException {
    byte[] data = new byte[1];
    when(folderManager.createWritableFile()).thenReturn(writableFile);
    doThrow(ResourceClosedException.class).when(writableFile).append(data);

    try {
      storage.write(data);
      fail();
    } catch (MaxAttemptsReachedException e) {
      verify(folderManager, times(3)).createWritableFile();
    }
  }
}
