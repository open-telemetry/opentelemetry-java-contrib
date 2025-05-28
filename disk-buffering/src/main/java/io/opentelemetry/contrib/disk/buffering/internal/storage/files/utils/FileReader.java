package io.opentelemetry.contrib.disk.buffering.internal.storage.files.utils;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;

public class FileReader implements Closeable {
  private final RandomAccessFile file;
  private final FileChannel channel;

  public static FileReader create(File file) throws IOException {
    RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rwd");
    FileChannel channel = randomAccessFile.getChannel();
    channel.force(false);
    return new FileReader(randomAccessFile, channel);
  }

  private FileReader(RandomAccessFile file, FileChannel channel) {
    this.file = file;
    this.channel = channel;
  }

  public int read() throws IOException {
    return file.read();
  }

  public int read(byte[] bytes) throws IOException {
    return file.read(bytes);
  }

  @Override
  public void close() throws IOException {
    channel.close();
    file.close();
  }

  public void truncateTop(long size) throws IOException {
    file.seek(size);
    truncateTop();
  }

  public void truncateTop() throws IOException {
    long position = file.getFilePointer();
    if (position == 0) {
      return;
    }
    byte[] remainingBytes = new byte[(int) (file.length() - position)];
    file.read(remainingBytes);
    channel.truncate(position);
    file.seek(0);
    file.write(remainingBytes);
    file.seek(0);
  }
}
