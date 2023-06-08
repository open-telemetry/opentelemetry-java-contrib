package io.opentelemetry.contrib.disk.buffering.internal.storage.files;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.function.Function;

public final class ReadableFile extends StorageFile {
  private final int originalFileSize;
  private final BufferedReader bufferedInputStream;
  private final FileChannel sourceOutChannel;
  private final FileChannel tempInChannel;
  private int readBytes = 0;

  @SuppressWarnings("resource")
  public ReadableFile(File file) throws IOException {
    super(file);
    originalFileSize = (int) file.length();
    Path readableFile = Files.createTempFile(file.getName() + "_", ".tmp");
    Files.copy(file.toPath(), readableFile, StandardCopyOption.REPLACE_EXISTING);
    FileInputStream tempInputStream = new FileInputStream(readableFile.toFile());
    tempInChannel = tempInputStream.getChannel();
    bufferedInputStream =
        new BufferedReader(new InputStreamReader(tempInputStream, StandardCharsets.UTF_8));
    sourceOutChannel = new FileOutputStream(file).getChannel();
  }

  public synchronized void readLine(Function<byte[], Boolean> consumer) throws IOException {
    byte[] line = bufferedInputStream.readLine().getBytes(StandardCharsets.UTF_8);
    if (consumer.apply(line)) {
      readBytes += line.length;
      sourceOutChannel.transferFrom(tempInChannel, readBytes, originalFileSize - readBytes);
    }
  }

  @Override
  public long getSize() {
    return originalFileSize;
  }

  @Override
  public boolean hasExpired() {
    return false;
  }

  @Override
  public synchronized void close() throws IOException {
    bufferedInputStream.close();
    sourceOutChannel.close();
  }
}
