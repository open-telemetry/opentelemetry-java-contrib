package io.opentelemetry.contrib.disk.buffering.internal.utils;

import com.squareup.wire.ProtoAdapter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public final class ProtobufTools {

  private ProtobufTools() {}

  public static void writeRawVarint32(int value, OutputStream out) throws IOException {
    ProtoAdapter.INT32.encode(out, value);
  }

  /** This code has been taken from Google's protobuf CodedInputStream. */
  public static int readRawVarint32(int firstByte, InputStream input) throws IOException {
    if ((firstByte & 0x80) == 0) {
      return firstByte;
    }

    int result = firstByte & 0x7f;
    int offset = 7;
    for (; offset < 32; offset += 7) {
      int b = input.read();
      if (b == -1) {
        throw new IllegalStateException();
      }
      result |= (b & 0x7f) << offset;
      if ((b & 0x80) == 0) {
        return result;
      }
    }
    // Keep reading up to 64 bits.
    for (; offset < 64; offset += 7) {
      int b = input.read();
      if (b == -1) {
        throw new IllegalStateException();
      }
      if ((b & 0x80) == 0) {
        return result;
      }
    }
    throw new IllegalStateException();
  }
}
