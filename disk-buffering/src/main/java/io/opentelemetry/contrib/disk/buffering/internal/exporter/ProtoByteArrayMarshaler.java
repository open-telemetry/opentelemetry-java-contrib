package io.opentelemetry.contrib.disk.buffering.internal.exporter;

import io.opentelemetry.exporter.internal.marshal.MarshalerWithSize;
import io.opentelemetry.exporter.internal.marshal.Serializer;
import java.io.IOException;

class ProtoByteArrayMarshaler extends MarshalerWithSize {

  private final byte[] bytes;

  ProtoByteArrayMarshaler(byte[] bytes) {
    super(bytes.length);
    this.bytes = bytes;
  }

  @Override
  protected void writeTo(Serializer output) throws IOException {
    output.writeSerializedMessage(bytes, "");
  }

  public byte[] getBytes() {
    return bytes;
  }
}
