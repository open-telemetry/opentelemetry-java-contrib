package io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.common;

import com.google.protobuf.ByteString;

public final class ByteStringMapper {

  public static final ByteStringMapper INSTANCE = new ByteStringMapper();

  public ByteString stringToProto(String source) {
    return ByteString.copyFromUtf8(source);
  }

  public String protoToString(ByteString source) {
    return source.toStringUtf8();
  }
}
