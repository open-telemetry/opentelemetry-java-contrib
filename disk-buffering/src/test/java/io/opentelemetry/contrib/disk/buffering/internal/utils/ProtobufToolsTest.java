/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.opentelemetry.contrib.disk.buffering.internal.serialization.deserializers.DeserializationException;
import org.junit.jupiter.api.Test;

class ProtobufToolsTest {

  @Test
  void countRepeatedField_emptyData() throws DeserializationException {
    byte[] data = {};
    int count = ProtobufTools.countRepeatedField(data, 1);
    assertThat(count).isEqualTo(0);
  }

  @Test
  void countRepeatedField_singleVarintField() throws DeserializationException {
    // Field 1, wire type 0 (varint), value 42
    byte[] data = {0x08, 0x2A}; // tag=8 (field 1, wire type 0), value=42
    int count = ProtobufTools.countRepeatedField(data, 1);
    assertThat(count).isEqualTo(1);
  }

  @Test
  void countRepeatedField_multipleVarintFields() throws DeserializationException {
    // Field 1 appears 3 times with different values
    byte[] data = {
      0x08, 0x01, // field 1, value 1
      0x08, 0x02, // field 1, value 2
      0x08, 0x03 // field 1, value 3
    };
    int count = ProtobufTools.countRepeatedField(data, 1);
    assertThat(count).isEqualTo(3);
  }

  @Test
  void countRepeatedField_singleFixed32Field() throws DeserializationException {
    // Field 2, wire type 5 (fixed32), value 0x12345678
    byte[] data = {
      0x15, 0x78, 0x56, 0x34, 0x12
    }; // tag=21 (field 2, wire type 5), little-endian value
    int count = ProtobufTools.countRepeatedField(data, 2);
    assertThat(count).isEqualTo(1);
  }

  @Test
  void countRepeatedField_multipleFixed32Fields() throws DeserializationException {
    // Field 2 appears twice
    byte[] data = {
      // spotless:off
      0x15, 0x78, 0x56, 0x34, 0x12,        // field 2, value 0x12345678
      0x15, (byte) 0x87, 0x65, 0x43, 0x21  // field 2, value 0x21436587
      // spotless:on
    };
    int count = ProtobufTools.countRepeatedField(data, 2);
    assertThat(count).isEqualTo(2);
  }

  @Test
  void countRepeatedField_singleFixed64Field() throws DeserializationException {
    // Field 3, wire type 1 (fixed64)
    byte[] data = {
      0x19, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08
    }; // tag=25 (field 3, wire type 1)
    int count = ProtobufTools.countRepeatedField(data, 3);
    assertThat(count).isEqualTo(1);
  }

  @Test
  void countRepeatedField_multipleLengthDelimitedFields() throws DeserializationException {
    // Field 4 appears twice as length-delimited (strings/messages)
    byte[] data = {
      0x22, 0x05, 'h', 'e', 'l', 'l', 'o', // field 4, length 5, "hello"
      0x22, 0x05, 'w', 'o', 'r', 'l', 'd' // field 4, length 5, "world"
    };
    int count = ProtobufTools.countRepeatedField(data, 4);
    assertThat(count).isEqualTo(2);
  }

  @Test
  void countRepeatedField_targetFieldNotPresent() throws DeserializationException {
    // Only field 1 is present, but we're looking for field 2
    byte[] data = {0x08, 0x2A}; // field 1, value 42
    int count = ProtobufTools.countRepeatedField(data, 2);
    assertThat(count).isEqualTo(0);
  }

  @Test
  void countRepeatedField_skipOtherFields() throws DeserializationException {
    // Multiple fields present, but we only count field 2
    byte[] data = {
      // spotless:off
      0x08, 0x01,                           // field 1, varint
      0x15, 0x78, 0x56, 0x34, 0x12,         // field 2, fixed32 (target)
      0x1A, 0x05, 'h', 'e', 'l', 'l', 'o',  // field 3, string
      0x15, (byte) 0x87, 0x65, 0x43, 0x21,  // field 2, fixed32 (target)
      0x20, 0x64                            // field 4, varint
      // spotless:on
    };
    int count = ProtobufTools.countRepeatedField(data, 2);
    assertThat(count).isEqualTo(2);
  }

  @Test
  void countRepeatedField_largeVarint() throws DeserializationException {
    // Test with a large varint that uses multiple bytes
    byte[] data = {
      0x08, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, 0x0F
    }; // field 1, large varint
    int count = ProtobufTools.countRepeatedField(data, 1);
    assertThat(count).isEqualTo(1);
  }

  @Test
  void countRepeatedField_emptyLengthDelimited() throws DeserializationException {
    // Field with empty length-delimited content
    byte[] data = {0x12, 0x00}; // field 2, length 0
    int count = ProtobufTools.countRepeatedField(data, 2);
    assertThat(count).isEqualTo(1);
  }

  @Test
  void countRepeatedField_nestedMessage() throws DeserializationException {
    // Field containing a nested message with its own fields
    byte[] nestedMessage = {0x08, 0x01, 0x10, 0x02}; // inner field 1=1, inner field 2=2
    byte[] data = new byte[2 + nestedMessage.length];
    data[0] = 0x1A; // field 3, wire type 2 (length-delimited)
    data[1] = (byte) nestedMessage.length;
    System.arraycopy(nestedMessage, 0, data, 2, nestedMessage.length);

    int count = ProtobufTools.countRepeatedField(data, 3);
    assertThat(count).isEqualTo(1);
  }

  @Test
  void countRepeatedField_truncatedVarint() {
    // Incomplete varint at end of data
    byte[] data = {0x08, (byte) 0xFF}; // field 1, incomplete varint (missing continuation)
    assertThatThrownBy(() -> ProtobufTools.countRepeatedField(data, 1))
        .isInstanceOf(DeserializationException.class)
        .hasMessageContaining("Truncated varint");
  }

  @Test
  void countRepeatedField_truncatedLengthDelimited() throws DeserializationException {
    // Length-delimited field with length > remaining data
    byte[] data = {0x12, 0x10, 0x01, 0x02}; // field 2, claims length 16 but only 2 bytes follow
    int count = ProtobufTools.countRepeatedField(data, 2);
    assertThat(count).isEqualTo(1);
  }

  @Test
  void countRepeatedField_invalidWireType() {
    // Wire type 6 is not valid
    byte[] data = {0x0E, 0x01}; // field 1, wire type 6 (invalid)
    assertThatThrownBy(() -> ProtobufTools.countRepeatedField(data, 1))
        .isInstanceOf(DeserializationException.class)
        .hasMessageContaining("Unsupported wire type: 6");
  }

  @Test
  void countRepeatedField_startGroupWireType() {
    // Wire type 3 (START_GROUP) is no longer supported
    byte[] data = {0x0B}; // field 1, wire type 3 (START_GROUP)
    assertThatThrownBy(() -> ProtobufTools.countRepeatedField(data, 1))
        .isInstanceOf(DeserializationException.class)
        .hasMessageContaining("Unsupported wire type: 3");
  }

  @Test
  void countRepeatedField_endGroupWireType() {
    // Wire type 4 (END_GROUP) is no longer supported
    byte[] data = {0x0C}; // field 1, wire type 4 (END_GROUP)
    assertThatThrownBy(() -> ProtobufTools.countRepeatedField(data, 1))
        .isInstanceOf(DeserializationException.class)
        .hasMessageContaining("Unsupported wire type: 4");
  }

  @Test
  void countRepeatedField_realWorldExample() throws DeserializationException {
    // Simulate a more realistic protobuf message structure
    // message ExportRequest {
    //   repeated SpanData spans = 1;
    //   string service_name = 2;
    // }
    // With 3 spans and a service name
    byte[] data = {
      // spotless:off
      // First span (field 1, length-delimited)
      0x0A, 0x04, 0x08, 0x01, 0x10, 0x02, // span 1: some inner fields
      // Second span (field 1, length-delimited)
      0x0A, 0x04, 0x08, 0x03, 0x10, 0x04, // span 2: some inner fields
      // Service name (field 2, length-delimited)
      0x12, 0x07, 's', 'e', 'r', 'v', 'i', 'c', 'e',
      // Third span (field 1, length-delimited)
      0x0A, 0x04, 0x08, 0x05, 0x10, 0x06  // span 3: some inner fields
      // spotless:on
    };

    // Count spans (field 1)
    int spanCount = ProtobufTools.countRepeatedField(data, 1);
    assertThat(spanCount).isEqualTo(3);

    // Count service names (field 2)
    int serviceNameCount = ProtobufTools.countRepeatedField(data, 2);
    assertThat(serviceNameCount).isEqualTo(1);
  }
}
