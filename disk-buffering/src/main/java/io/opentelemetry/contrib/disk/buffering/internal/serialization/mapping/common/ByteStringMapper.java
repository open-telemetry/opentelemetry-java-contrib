/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.common;

import okio.ByteString;

/**
 * Maps between hex strings and proto {@link ByteString} values.
 *
 * <p>This class is internal and is hence not for public use. Its APIs are unstable and can change
 * at any time.
 */
public final class ByteStringMapper {

  private static final ByteStringMapper INSTANCE = new ByteStringMapper();

  public static ByteStringMapper getInstance() {
    return INSTANCE;
  }

  public ByteString stringToProto(String source) {
    return ByteString.decodeHex(source);
  }

  public String protoToString(ByteString source) {
    return source.hex();
  }
}
