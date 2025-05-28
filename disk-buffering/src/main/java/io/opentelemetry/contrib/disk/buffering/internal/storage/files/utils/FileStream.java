package io.opentelemetry.contrib.disk.buffering.internal.storage.files.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import org.jetbrains.annotations.NotNull;

public class FileStream extends InputStream {
  private final RandomAccessFile file;
  private final FileChannel channel;

  public static FileStream create(File file) throws IOException {
    RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rwd");
    FileChannel channel = randomAccessFile.getChannel();
    channel.force(false);
    return new FileStream(randomAccessFile, channel);
  }

  private FileStream(RandomAccessFile file, FileChannel channel) {
    this.file = file;
    this.channel = channel;
  }

  @Override
  public int read() throws IOException {
    return file.read();
  }

  @Override
  public int read(@NotNull byte[] bytes) throws IOException {
    return file.read(bytes);
  }

  @Override
  public int read(@NotNull byte[] b, int off, int len) throws IOException {
    return file.read(b, off, len);
  }

  public long size() throws IOException {
    return channel.size();
  }

  @Override
  public void close() throws IOException {
    channel.close();
    file.close();
  }

  public void truncateTop(long size) throws IOException {
    file.seek(Math.min(size(), size));
    truncateTop();
  }

  public void truncateTop() throws IOException {
    long position = file.getFilePointer();
    if (position == 0) {
      return;
    }
    long remainingSize = size() - position;
    if (remainingSize > 0) {
      byte[] remainingBytes = new byte[(int) remainingSize];
      file.read(remainingBytes);
      file.seek(0);
      channel.truncate(position);
      file.write(remainingBytes);
      file.seek(0);
    } else {
      channel.truncate(0);
    }
  }

  public long getPosition() throws IOException {
    return file.getFilePointer();
  }
}
