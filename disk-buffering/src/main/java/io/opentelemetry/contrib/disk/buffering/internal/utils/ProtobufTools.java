/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.utils;

import com.squareup.wire.ProtoAdapter;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.deserializers.DeserializationException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public final class ProtobufTools {

  // Wire types
  private static final int WIRETYPE_VARINT = 0;
  private static final int WIRETYPE_FIXED64 = 1;
  private static final int WIRETYPE_LENGTH_DELIMITED = 2;
  private static final int WIRETYPE_FIXED32 = 5;

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

  /**
   * Vendored {@link Byte#toUnsignedInt(byte)} to support Android. Also helps with accidental sign
   * propagation.
   */
  public static int toUnsignedInt(byte x) {
    return ((int) x) & 0xff;
  }

  /**
   * Counts repeated field occurrences in a protobuf-encoded byte array. Handles both packed and
   * unpacked encodings.
   */
  public static int countRepeatedField(byte[] data, int targetField)
      throws DeserializationException {
    int i = 0;
    int count = 0;

    while (i < data.length) {
      // Read tag (varint)
      long[] tagAndLen = readVarint(data, i);
      long tag = tagAndLen[0];
      int n = (int) tagAndLen[1];
      if (tag == 0) {
        break; // end marker
      }

      i += n;
      int fieldNumber = (int) (tag >>> 3);
      int wireType = (int) (tag & 0x07);

      if (fieldNumber == targetField) {
        switch (wireType) {
          case WIRETYPE_VARINT:
            long[] v = readVarint(data, i);
            i += (int) v[1];
            count++;
            break;
          case WIRETYPE_FIXED64:
            i += 8;
            count++;
            break;
          case WIRETYPE_FIXED32:
            i += 4;
            count++;
            break;
          case WIRETYPE_LENGTH_DELIMITED:
            long[] lres = readVarint(data, i);
            int len = (int) lres[0];
            int ln = (int) lres[1];
            i += ln;

            // Each length-delimited field occurrence counts as one element
            // (message, string, bytes, or unpacked repeated field element)
            count++;
            i += len;
            break;
          default:
            throw new DeserializationException("Unsupported wire type: " + wireType);
        }
      } else {
        // skip unknown field
        i = skipField(data, i, wireType);
      }
    }
    return count;
  }

  /** Reads a varint starting at offset. Returns [value, lengthInBytes]. */
  private static long[] readVarint(byte[] data, int offset) throws DeserializationException {
    long result = 0;
    int shift = 0;
    int i = offset;
    while (i < data.length) {
      byte b = data[i++];
      result |= (long) (b & 0x7F) << shift;
      if ((b & 0x80) == 0) {
        return new long[] {result, i - offset};
      }
      shift += 7;
      if (shift > 64) {
        throw new DeserializationException("Varint too long");
      }
    }
    throw new DeserializationException("Truncated varint");
  }

  /** Skips a field of given wire type. */
  private static int skipField(byte[] data, int offset, int wireType)
      throws DeserializationException {
    switch (wireType) {
      case WIRETYPE_VARINT:
        long[] v = readVarint(data, offset);
        return offset + (int) v[1];
      case WIRETYPE_FIXED64:
        return offset + 8;
      case WIRETYPE_FIXED32:
        return offset + 4;
      case WIRETYPE_LENGTH_DELIMITED:
        long[] lres = readVarint(data, offset);
        int len = (int) lres[0];
        int ln = (int) lres[1];
        return offset + ln + len;
      default:
        throw new DeserializationException("Unsupported wire type: " + wireType);
    }
  }
}
