/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.common;

import okio.ByteString;

public final class ByteStringMapper {

  private static final ByteStringMapper INSTANCE = new ByteStringMapper();

  public static ByteStringMapper getInstance() {
    return INSTANCE;
  }

  public ByteString stringToProto(String source) {
    return ByteString.encodeUtf8(source);
  }

  public String protoToString(ByteString source) {
    return source.utf8();
  }
}
