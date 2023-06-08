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
import io.opentelemetry.contrib.disk.buffering.internal.storage.exceptions.ReadingTimeoutException;
import io.opentelemetry.contrib.disk.buffering.internal.storage.exceptions.StorageClosedException;
import io.opentelemetry.contrib.disk.buffering.internal.storage.files.ReadableFile;
import java.io.IOException;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StorageTest {
  private FolderManager folderManager;
  private Storage storage;
  private Function<byte[], Boolean> consumer;
  private ReadableFile readableFile;

  @BeforeEach
  public void setUp() {
    folderManager = mock();
    consumer = mock();
    readableFile = mock();
    storage = new Storage(folderManager);
  }

  @Test
  public void whenReadingSuccessfully_returnTrue() throws IOException {
    doReturn(readableFile).when(folderManager).getReadableFile();

    assertTrue(storage.read(consumer));

    verify(readableFile).readLine(consumer);
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
  public void whenStorageClosedExceptionHappens_lookForNewFileToRead() throws IOException {
    when(folderManager.getReadableFile()).thenReturn(readableFile).thenReturn(null);
    doThrow(StorageClosedException.class).when(readableFile).readLine(consumer);

    assertFalse(storage.read(consumer));

    verify(folderManager, times(2)).getReadableFile();
  }

  @Test
  public void whenEveryNewFileFoundCannotBeRead_stopAfterMaxAttempts() throws IOException {
    when(folderManager.getReadableFile()).thenReturn(readableFile);
    doThrow(StorageClosedException.class).when(readableFile).readLine(consumer);

    try {
      assertFalse(storage.read(consumer));
      fail();
    } catch (MaxAttemptsReachedException e) {
      verify(folderManager, times(10)).getReadableFile();
    }
  }
}
