/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.inferredspans.util;

public class HexUtils {

  public static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();

  private HexUtils() {
    // only static utility methods, don't instantiate
  }

  public static void appendLongAsHex(long value, StringBuilder builder) {
    appendHexChar(value >> 60, builder);
    appendHexChar(value >> 56, builder);
    appendHexChar(value >> 52, builder);
    appendHexChar(value >> 48, builder);
    appendHexChar(value >> 44, builder);
    appendHexChar(value >> 40, builder);
    appendHexChar(value >> 36, builder);
    appendHexChar(value >> 32, builder);
    appendHexChar(value >> 28, builder);
    appendHexChar(value >> 24, builder);
    appendHexChar(value >> 20, builder);
    appendHexChar(value >> 16, builder);
    appendHexChar(value >> 12, builder);
    appendHexChar(value >> 8, builder);
    appendHexChar(value >> 4, builder);
    appendHexChar(value, builder);
  }

  private static void appendHexChar(long value, StringBuilder sb) {
    sb.append(HEX_CHARS[(int) (value & 0x0F)]);
  }

  public static long hexToLong(CharSequence hex, int offset) {
    if (hex.length() - offset < 16) {
      throw new IllegalStateException("Provided hex string '" + hex + "' is too short");
    }
    return hexCharToBinary(hex.charAt(offset)) << 60
        | hexCharToBinary(hex.charAt(offset + 1)) << 56
        | hexCharToBinary(hex.charAt(offset + 2)) << 52
        | hexCharToBinary(hex.charAt(offset + 3)) << 48
        | hexCharToBinary(hex.charAt(offset + 4)) << 44
        | hexCharToBinary(hex.charAt(offset + 5)) << 40
        | hexCharToBinary(hex.charAt(offset + 6)) << 36
        | hexCharToBinary(hex.charAt(offset + 7)) << 32
        | hexCharToBinary(hex.charAt(offset + 8)) << 28
        | hexCharToBinary(hex.charAt(offset + 9)) << 24
        | hexCharToBinary(hex.charAt(offset + 10)) << 20
        | hexCharToBinary(hex.charAt(offset + 11)) << 16
        | hexCharToBinary(hex.charAt(offset + 12)) << 12
        | hexCharToBinary(hex.charAt(offset + 13)) << 8
        | hexCharToBinary(hex.charAt(offset + 14)) << 4
        | hexCharToBinary(hex.charAt(offset + 15));
  }

  private static long hexCharToBinary(char ch) {
    if ('0' <= ch && ch <= '9') {
      return ch - '0';
    }
    if ('A' <= ch && ch <= 'F') {
      return ch - 'A' + 10;
    }
    if ('a' <= ch && ch <= 'f') {
      return ch - 'a' + 10;
    }
    throw new IllegalArgumentException("Not a hex char: " + ch);
  }
}
