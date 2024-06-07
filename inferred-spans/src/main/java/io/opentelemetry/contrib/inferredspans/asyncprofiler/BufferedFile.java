/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.inferredspans.asyncprofiler;

import io.opentelemetry.contrib.inferredspans.pooling.Recyclable;
import java.io.File;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.StandardOpenOption;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;

/**
 * An abstraction similar to {@link MappedByteBuffer} that allows to read the content of a file with
 * an API that is similar to {@link ByteBuffer}.
 *
 * <p>Instances of this class hold a reusable buffer that contains a subset of the file, or the
 * whole file if the buffer's capacity is greater or equal to the file's size.
 *
 * <p>Whenever calling a method like {@link #getLong()} or {@link #position(long)} would exceed the
 * currently buffered range the same buffer is filled with a different range of the file.
 *
 * <p>The downside of {@link MappedByteBuffer} (and the reason for implementing this abstraction) is
 * that calling methods like {@link MappedByteBuffer#get()} can increase time-to-safepoint. This is
 * because these methods are implemented as JVM intrinsics. When the JVM executes an intrinsic, it
 * does not switch to the native execution context which means that it's not ready to enter a
 * safepoint whenever a intrinsic runs. As reading a file from disk can get stuck (for example when
 * the disk is busy) calling {@link MappedByteBuffer#get()} may take a while to execute. While it's
 * executing other threads have to wait for it to finish if the JVM wants to reach a safe point.
 */
class BufferedFile implements Recyclable {

  private final Logger logger = Logger.getLogger(BufferedFile.class.getName());

  private static final int SIZE_OF_BYTE = 1;
  private static final int SIZE_OF_SHORT = 2;
  private static final int SIZE_OF_INT = 4;
  private static final int SIZE_OF_LONG = 8;

  // The following constant are defined by the JFR file format for identifying the string encoding
  private static final int STRING_ENCODING_NULL = 0;
  private static final int STRING_ENCODING_EMPTY = 1;
  private static final int STRING_ENCODING_CONSTANTPOOL = 2;
  private static final int STRING_ENCODING_UTF8 = 3;
  private static final int STRING_ENCODING_CHARARRAY = 4;
  private static final int STRING_ENCODING_LATIN1 = 5;

  private ByteBuffer buffer;
  private final ByteBuffer bigBuffer;
  private final ByteBuffer smallBuffer;

  /** The offset of the file from where the {@link #buffer} starts */
  private long offset;

  private boolean wholeFileInBuffer;
  @Nullable private FileChannel fileChannel;

  /**
   * @param bigBuffer the buffer to be used to read the whole file if the file fits into it
   * @param smallBuffer the buffer to be used to read chunks of the file in case the file is larger
   *     than bigBuffer. Constantly seeking a file with a large buffer is very bad for performance.
   */
  @SuppressWarnings("NullAway")
  public BufferedFile(ByteBuffer bigBuffer, ByteBuffer smallBuffer) {
    this.bigBuffer = bigBuffer;
    this.smallBuffer = smallBuffer;
  }

  /**
   * Sets the file and depending on it's size, may read the file into the {@linkplain #buffer
   * buffer}
   *
   * @param file the file to read from
   * @throws IOException If some I/O error occurs
   */
  public void setFile(File file) throws IOException {
    fileChannel = FileChannel.open(file.toPath(), StandardOpenOption.READ);
    if (fileChannel.size() <= bigBuffer.capacity()) {
      buffer = bigBuffer;
      read(0, bigBuffer.capacity());
      wholeFileInBuffer = true;
    } else {
      buffer = smallBuffer;
      Buffer buffer = this.buffer;
      buffer.flip();
    }
  }

  /**
   * Skips the provided number of bytes in the file without reading new data.
   *
   * @param bytesToSkip the number of bytes to skip
   */
  public void skip(int bytesToSkip) {
    position(position() + bytesToSkip);
  }

  public void skipString() throws IOException {
    readOrSkipString(get(), null);
  }

  /**
   * Reads a JFR string.
   *
   * @param output the buffer to place the string intro
   * @return false, if the string to read is null, true otherwise
   */
  public boolean readString(StringBuilder output) throws IOException {
    byte encoding = get();
    if (encoding == STRING_ENCODING_NULL) {
      return false;
    }
    readOrSkipString(encoding, output);
    return true;
  }

  @Nullable
  public String readString() throws IOException {
    byte encoding = get();
    if (encoding == STRING_ENCODING_NULL) {
      return null;
    }
    if (encoding == STRING_ENCODING_EMPTY) {
      return "";
    }
    StringBuilder output = new StringBuilder();
    readOrSkipString(encoding, output);
    return output.toString();
  }

  private void readOrSkipString(byte encoding, @Nullable StringBuilder output) throws IOException {
    switch (encoding) {
      case STRING_ENCODING_NULL:
      case STRING_ENCODING_EMPTY:
        return;
      case STRING_ENCODING_CONSTANTPOOL:
        if (output != null) {
          throw new IllegalStateException("Reading constant pool string is not supported");
        }
        getVarLong();
        return;
      case STRING_ENCODING_UTF8:
        readOrSkipUtf8(output);
        return;
      case STRING_ENCODING_CHARARRAY:
        throw new IllegalStateException("Char-array encoding is not supported by the parser yet");
      case STRING_ENCODING_LATIN1:
        if (output != null) {
          throw new IllegalStateException("Reading LATIN1 encoded string is not supported");
        }
        skip(getVarInt());
        return;
      default:
        throw new IllegalStateException("Unknown string encoding type: " + encoding);
    }
  }

  private void readOrSkipUtf8(@Nullable StringBuilder output) throws IOException {
    int len = getVarInt();
    if (output == null) {
      skip(len);
      return;
    }
    ensureRemaining(len, len);

    for (int i = 0; i < len; i++) {
      byte hopefullyAscii = getUnsafe();
      if (hopefullyAscii > 0) {
        output.append((char) hopefullyAscii);
      } else {
        // encountered non-ascii character: fallback to allocating and UTF8-decoding
        position(position() - 1); // reset position before the just read byte
        byte[] utf8Data = new byte[len - i];
        buffer.get(utf8Data);
        output.append(new String(utf8Data, StandardCharsets.UTF_8));
        return;
      }
    }
  }

  /**
   * Returns the position of the file
   *
   * @return the position of the file
   */
  public long position() {
    return offset + buffer.position();
  }

  /**
   * Sets the position of the file without reading new data.
   *
   * @param pos the new position
   */
  public void position(long pos) {
    Buffer buffer = this.buffer;
    long positionDelta = pos - position();
    long newBufferPos = buffer.position() + positionDelta;
    if (0 <= newBufferPos && newBufferPos <= buffer.limit()) {
      buffer.position((int) newBufferPos);
    } else {
      // makes sure that the next ensureRemaining will load from file
      buffer.position(0);
      buffer.limit(0);
      offset = pos;
    }
  }

  /**
   * Ensures that the provided number of bytes are available in the {@linkplain #buffer buffer}
   *
   * @param minRemaining the number of bytes which are guaranteed to be available in the {@linkplain
   *     #buffer buffer}
   * @throws IOException If some I/O error occurs
   * @throws IllegalStateException If minRemaining is greater than the buffer's capacity
   */
  public void ensureRemaining(int minRemaining) throws IOException {
    ensureRemaining(minRemaining, buffer.capacity());
  }

  /**
   * Ensures that the provided number of bytes are available in the {@linkplain #buffer buffer}
   *
   * @param minRemaining the number of bytes which are guaranteed to be available in the {@linkplain
   *     #buffer buffer}
   * @param maxRead the max number of bytes to read from the file in case the buffer does currently
   *     not hold {@code minRemaining} bytes
   * @throws IOException If some I/O error occurs
   * @throws IllegalStateException If minRemaining is greater than the buffer's capacity
   */
  public void ensureRemaining(int minRemaining, int maxRead) throws IOException {
    if (wholeFileInBuffer) {
      return;
    }
    if (minRemaining > buffer.capacity()) {
      throw new IllegalStateException(
          String.format(
              "Length (%d) greater than buffer capacity (%d)", minRemaining, buffer.capacity()));
    }
    if (buffer.remaining() < minRemaining) {
      read(position(), maxRead);
    }
  }

  /**
   * Gets a byte from the current {@linkplain #position() position} of this file. If the {@linkplain
   * #buffer buffer} does not fully contain this byte, loads another slice of the file into the
   * buffer.
   *
   * @return The byte at the file's current position
   * @throws IOException If some I/O error occurs
   */
  public byte get() throws IOException {
    ensureRemaining(SIZE_OF_BYTE);
    return buffer.get();
  }

  /**
   * Gets a short from the current {@linkplain #position() position} of this file. If the
   * {@linkplain #buffer buffer} does not fully contain this short, loads another slice of the file
   * into the buffer.
   *
   * @return The short at the file's current position
   * @throws IOException If some I/O error occurs
   */
  public short getShort() throws IOException {
    ensureRemaining(SIZE_OF_SHORT);
    return buffer.getShort();
  }

  /**
   * Gets a short from the current {@linkplain #position() position} of this file. If the
   * {@linkplain #buffer buffer} does not fully contain this short, loads another slice of the file
   * into the buffer.
   *
   * @return The short at the file's current position
   * @throws IOException If some I/O error occurs
   */
  public int getUnsignedShort() throws IOException {
    return getShort() & 0xffff;
  }

  /**
   * Gets a int from the current {@linkplain #position() position} of this file and converts it to
   * an unsigned short. If the {@linkplain #buffer buffer} does not fully contain this int, loads
   * another slice of the file into the buffer.
   *
   * @return The int at the file's current position
   * @throws IOException If some I/O error occurs
   */
  public int getInt() throws IOException {
    ensureRemaining(SIZE_OF_INT);
    return buffer.getInt();
  }

  /**
   * Gets a long from the current {@linkplain #position() position} of this file. If the {@linkplain
   * #buffer buffer} does not fully contain this long, loads another slice of the file into the
   * buffer.
   *
   * @return The long at the file's current position
   * @throws IOException If some I/O error occurs
   */
  public long getLong() throws IOException {
    ensureRemaining(SIZE_OF_LONG);
    return buffer.getLong();
  }

  /** Reads LEB-128 variable length encoded values of a size of up to 64 bit. */
  public long getVarLong() throws IOException {
    long value = 0;
    boolean hasNext = true;
    int shift = 0;
    while (hasNext) {
      long byteVal = ((int) get());
      hasNext = (byteVal & 0x80) != 0;
      value |= (byteVal & 0x7F) << shift;
      shift += 7;
    }
    return value;
  }

  public int getVarInt() throws IOException {
    long val = getVarLong();
    if ((int) val != val) {
      throw new IllegalArgumentException("The LEB128 encoded value does not fit in an int");
    }
    return (int) val;
  }

  /**
   * Gets a byte from the underlying buffer without checking if this part of the file is actually in
   * the buffer.
   *
   * <p>Always mare sure to call {@link #ensureRemaining} before.
   *
   * @return The byte at the file's current position
   * @throws java.nio.BufferUnderflowException If the buffer's current position is not smaller than
   *     its limit
   */
  public byte getUnsafe() {
    return buffer.get();
  }

  /**
   * Gets a short from the underlying buffer without checking if this part of the file is actually
   * in the buffer.
   *
   * <p>Always mare sure to call {@link #ensureRemaining} before.
   *
   * @return The byte at the file's current position
   * @throws java.nio.BufferUnderflowException If there are fewer than two bytes remaining in this
   *     buffer
   */
  public short getUnsafeShort() {
    return buffer.getShort();
  }

  /**
   * Gets an int from the underlying buffer without checking if this part of the file is actually in
   * the buffer.
   *
   * <p>Always mare sure to call {@link #ensureRemaining} before.
   *
   * @return The byte at the file's current position
   * @throws java.nio.BufferUnderflowException If there are fewer than four bytes remaining in this
   *     buffer
   */
  public int getUnsafeInt() {
    return buffer.getInt();
  }

  /**
   * Gets a long from the underlying buffer without checking if this part of the file is actually in
   * the buffer.
   *
   * <p>Always mare sure to call {@link #ensureRemaining} before.
   *
   * @return The byte at the file's current position
   * @throws java.nio.BufferUnderflowException If there are fewer than eight bytes remaining in this
   *     buffer
   */
  public long getUnsafeLong() {
    return buffer.getLong();
  }

  public long size() throws IOException {
    if (fileChannel == null) {
      throw new IllegalStateException("setFile has not been called yet");
    }
    return fileChannel.size();
  }

  public boolean isSet() {
    return fileChannel != null;
  }

  @Override
  @SuppressWarnings("NullAway")
  public void resetState() {
    if (fileChannel == null) {
      throw new IllegalStateException("setFile has not been called yet");
    }
    Buffer buffer = this.buffer;
    buffer.clear();
    offset = 0;
    wholeFileInBuffer = false;
    try {
      fileChannel.close();
    } catch (IOException ignore) {
      logger.log(Level.FINE, "Ignored exception on file close", ignore);
    }
    fileChannel = null;
    this.buffer = null;
  }

  private void read(long offset, int limit) throws IOException {
    if (limit > buffer.capacity()) {
      limit = buffer.capacity();
    }
    Buffer buffer = this.buffer;
    buffer.clear();
    assert fileChannel != null;
    fileChannel.position(offset);
    buffer.limit(limit);
    fileChannel.read(this.buffer);
    buffer.flip();
    this.offset = offset;
  }
}
