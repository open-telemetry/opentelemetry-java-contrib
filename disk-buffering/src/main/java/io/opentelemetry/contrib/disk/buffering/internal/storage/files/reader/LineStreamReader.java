package io.opentelemetry.contrib.disk.buffering.internal.storage.files.reader;

import static io.opentelemetry.contrib.disk.buffering.internal.storage.files.utils.Constants.NEW_LINE_BYTES_SIZE;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import javax.annotation.Nullable;

public final class LineStreamReader extends StreamReader {
  private final BufferedReader bufferedReader;

  public LineStreamReader(InputStream inputStream) {
    super(inputStream);
    bufferedReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
  }

  @Override
  @Nullable
  public ReadResult read() throws IOException {
    String line = bufferedReader.readLine();
    if (line == null) {
      return null;
    }
    byte[] bytes = line.getBytes(StandardCharsets.UTF_8);
    return new ReadResult(bytes, bytes.length + NEW_LINE_BYTES_SIZE);
  }

  public static class Factory implements StreamReader.Factory {

    public static final Factory INSTANCE = new LineStreamReader.Factory();

    private Factory() {}

    @Override
    public StreamReader create(InputStream stream) {
      return new LineStreamReader(stream);
    }
  }
}
