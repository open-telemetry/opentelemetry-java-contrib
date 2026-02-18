/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.common;

import static java.util.Collections.emptyList;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.AttributeType;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.common.Value;
import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.common.v1.ArrayValue;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.common.v1.KeyValueList;
import okio.ByteString;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public final class AttributesMapper {

  private static final AttributesMapper INSTANCE = new AttributesMapper();

  /** Represents the type of value stored in a proto AnyValue. */
  private enum ProtoValueType {
    STRING,
    BOOL,
    INT,
    DOUBLE,
    BYTES,
    ARRAY,
    KVLIST,
    EMPTY
  }

  public static AttributesMapper getInstance() {
    return INSTANCE;
  }

  public List<KeyValue> attributesToProto(Attributes attributes) {
    List<KeyValue> keyValues = new ArrayList<>();
    attributes.forEach((attributeKey, o) -> keyValues.add(attributeEntryToProto(attributeKey, o)));
    return keyValues;
  }

  public Attributes protoToAttributes(List<KeyValue> values) {
    AttributesBuilder builder = Attributes.builder();
    for (KeyValue keyValue : values) {
      addValue(builder, keyValue.key, keyValue.value);
    }
    return builder.build();
  }

  private static KeyValue attributeEntryToProto(AttributeKey<?> key, Object value) {
    KeyValue.Builder builder = new KeyValue.Builder();
    builder.key(key.getKey());
    builder.value(attributeValueToProto(key.getType(), value));
    return builder.build();
  }

  // Type is checked via AttributeType before casting
  @SuppressWarnings("unchecked")
  private static AnyValue attributeValueToProto(AttributeType type, Object value) {
    switch (type) {
      case STRING:
        return stringToAnyValue((String) value);
      case BOOLEAN:
        return booleanToAnyValue((Boolean) value);
      case LONG:
        return longToAnyValue((Long) value);
      case DOUBLE:
        return doubleToAnyValue((Double) value);
      case STRING_ARRAY:
        return arrayToAnyValue(stringListToAnyValue((List<String>) value));
      case BOOLEAN_ARRAY:
        return arrayToAnyValue(booleanListToAnyValue((List<Boolean>) value));
      case LONG_ARRAY:
        return arrayToAnyValue(longListToAnyValue((List<Long>) value));
      case DOUBLE_ARRAY:
        return arrayToAnyValue(doubleListToAnyValue((List<Double>) value));
      case VALUE:
        return valueToAnyValue((Value<?>) value);
    }
    throw new UnsupportedOperationException();
  }

  // Type is checked via ValueType before casting
  @SuppressWarnings("unchecked")
  private static AnyValue valueToAnyValue(Value<?> value) {
    switch (value.getType()) {
      case STRING:
        return stringToAnyValue((String) value.getValue());
      case BOOLEAN:
        return booleanToAnyValue((Boolean) value.getValue());
      case LONG:
        return longToAnyValue((Long) value.getValue());
      case DOUBLE:
        return doubleToAnyValue((Double) value.getValue());
      case BYTES:
        ByteBuffer byteBuffer = (ByteBuffer) value.getValue();
        byte[] bytes = new byte[byteBuffer.remaining()];
        byteBuffer.get(bytes);
        return bytesToAnyValue(bytes);
      case ARRAY:
        List<Value<?>> arrayValues = (List<Value<?>>) value.getValue();
        return arrayValueToAnyValue(arrayValues);
      case KEY_VALUE_LIST:
        List<io.opentelemetry.api.common.KeyValue> kvList =
            (List<io.opentelemetry.api.common.KeyValue>) value.getValue();
        return keyValueListToAnyValue(kvList);
      case EMPTY:
        return new AnyValue.Builder().build();
    }
    throw new UnsupportedOperationException("Unsupported ValueType: " + value.getType());
  }

  private static AnyValue bytesToAnyValue(byte[] bytes) {
    AnyValue.Builder anyValue = new AnyValue.Builder();
    anyValue.bytes_value(ByteString.of(bytes));
    return anyValue.build();
  }

  private static AnyValue arrayValueToAnyValue(List<Value<?>> values) {
    List<AnyValue> anyValues = new ArrayList<>(values.size());
    for (Value<?> v : values) {
      anyValues.add(valueToAnyValue(v));
    }
    return new AnyValue.Builder()
        .array_value(new ArrayValue.Builder().values(anyValues).build())
        .build();
  }

  private static AnyValue keyValueListToAnyValue(
      List<io.opentelemetry.api.common.KeyValue> kvList) {
    List<KeyValue> protoKeyValues = new ArrayList<>(kvList.size());
    for (io.opentelemetry.api.common.KeyValue kv : kvList) {
      KeyValue.Builder kvBuilder = new KeyValue.Builder();
      kvBuilder.key(kv.getKey());
      kvBuilder.value(valueToAnyValue(kv.getValue()));
      protoKeyValues.add(kvBuilder.build());
    }
    return new AnyValue.Builder()
        .kvlist_value(new KeyValueList.Builder().values(protoKeyValues).build())
        .build();
  }

  private static AnyValue arrayToAnyValue(List<AnyValue> value) {
    return new AnyValue.Builder()
        .array_value(new ArrayValue.Builder().values(value).build())
        .build();
  }

  private static void addValue(AttributesBuilder builder, String key, AnyValue value) {
    if (value.string_value != null) {
      builder.put(AttributeKey.stringKey(key), value.string_value);
    } else if (value.bool_value != null) {
      builder.put(AttributeKey.booleanKey(key), value.bool_value);
    } else if (value.int_value != null) {
      builder.put(AttributeKey.longKey(key), value.int_value);
    } else if (value.double_value != null) {
      builder.put(AttributeKey.doubleKey(key), value.double_value);
    } else if (value.array_value != null) {
      addArray(builder, key, value.array_value);
    } else if (value.bytes_value != null) {
      builder.put(AttributeKey.valueKey(key), Value.of(value.bytes_value.toByteArray()));
    } else if (value.kvlist_value != null) {
      builder.put(AttributeKey.valueKey(key), anyValueToValue(value));
    } else {
      // Update after SDK v1.60.0 is released which includes:
      // https://github.com/open-telemetry/opentelemetry-java/pull/8014
      builder.put(AttributeKey.stringKey(key), "");
    }
  }

  private static Value<?> anyValueToValue(AnyValue value) {
    if (value.string_value != null) {
      return Value.of(value.string_value);
    } else if (value.bool_value != null) {
      return Value.of(value.bool_value);
    } else if (value.int_value != null) {
      return Value.of(value.int_value);
    } else if (value.double_value != null) {
      return Value.of(value.double_value);
    } else if (value.bytes_value != null) {
      return Value.of(value.bytes_value.toByteArray());
    } else if (value.array_value != null) {
      List<Value<?>> values = new ArrayList<>();
      for (AnyValue anyValue : value.array_value.values) {
        values.add(anyValueToValue(anyValue));
      }
      return Value.of(values);
    } else if (value.kvlist_value != null) {
      List<io.opentelemetry.api.common.KeyValue> kvList = new ArrayList<>();
      for (KeyValue kv : value.kvlist_value.values) {
        kvList.add(io.opentelemetry.api.common.KeyValue.of(kv.key, anyValueToValue(kv.value)));
      }
      return Value.of(kvList.toArray(new io.opentelemetry.api.common.KeyValue[0]));
    } else {
      return Value.empty();
    }
  }

  private static void addArray(AttributesBuilder builder, String key, ArrayValue arrayValue) {
    List<AnyValue> values = arrayValue.values;

    // Per SDK behavior (ArrayBackedAttributesBuilder#attributeType):
    // "VALUE if the array is empty, non-homogeneous, or contains unsupported element types"
    if (values.isEmpty()) {
      builder.put(AttributeKey.valueKey(key), Value.of(emptyList()));
      return;
    }

    // Check if array is homogeneous and of a primitive type
    AnyValue firstValue = values.get(0);
    boolean isHomogeneous = true;
    ProtoValueType arrayType = getProtoValueType(firstValue);

    for (int i = 1; i < values.size(); i++) {
      if (getProtoValueType(values.get(i)) != arrayType) {
        isHomogeneous = false;
        break;
      }
    }

    // If homogeneous and primitive, use typed array keys
    if (isHomogeneous) {
      if (firstValue.string_value != null) {
        builder.put(AttributeKey.stringArrayKey(key), anyValuesToStrings(values));
        return;
      } else if (firstValue.bool_value != null) {
        builder.put(AttributeKey.booleanArrayKey(key), anyValuesToBooleans(values));
        return;
      } else if (firstValue.int_value != null) {
        builder.put(AttributeKey.longArrayKey(key), anyValuesToLongs(values));
        return;
      } else if (firstValue.double_value != null) {
        builder.put(AttributeKey.doubleArrayKey(key), anyValuesToDoubles(values));
        return;
      }
    }

    // Heterogeneous or complex array - use VALUE type
    AnyValue anyValue = new AnyValue.Builder().array_value(arrayValue).build();
    builder.put(AttributeKey.valueKey(key), anyValueToValue(anyValue));
  }

  private static ProtoValueType getProtoValueType(AnyValue value) {
    if (value.string_value != null) {
      return ProtoValueType.STRING;
    } else if (value.bool_value != null) {
      return ProtoValueType.BOOL;
    } else if (value.int_value != null) {
      return ProtoValueType.INT;
    } else if (value.double_value != null) {
      return ProtoValueType.DOUBLE;
    } else if (value.bytes_value != null) {
      return ProtoValueType.BYTES;
    } else if (value.array_value != null) {
      return ProtoValueType.ARRAY;
    } else if (value.kvlist_value != null) {
      return ProtoValueType.KVLIST;
    } else {
      return ProtoValueType.EMPTY;
    }
  }

  private static AnyValue stringToAnyValue(String value) {
    AnyValue.Builder anyValue = new AnyValue.Builder();

    anyValue.string_value(value);

    return anyValue.build();
  }

  private static AnyValue booleanToAnyValue(Boolean value) {
    AnyValue.Builder anyValue = new AnyValue.Builder();

    if (value != null) {
      anyValue.bool_value(value);
    }

    return anyValue.build();
  }

  private static AnyValue longToAnyValue(Long value) {
    AnyValue.Builder anyValue = new AnyValue.Builder();

    if (value != null) {
      anyValue.int_value(value);
    }

    return anyValue.build();
  }

  private static AnyValue doubleToAnyValue(Double value) {
    AnyValue.Builder anyValue = new AnyValue.Builder();

    if (value != null) {
      anyValue.double_value(value);
    }

    return anyValue.build();
  }

  private static List<AnyValue> stringListToAnyValue(List<String> value) {
    List<AnyValue> list = new ArrayList<>(value.size());
    for (String string : value) {
      list.add(stringToAnyValue(string));
    }

    return list;
  }

  private static List<AnyValue> booleanListToAnyValue(List<Boolean> value) {
    List<AnyValue> list = new ArrayList<>(value.size());
    for (Boolean boolean1 : value) {
      list.add(booleanToAnyValue(boolean1));
    }

    return list;
  }

  private static List<AnyValue> longListToAnyValue(List<Long> value) {
    List<AnyValue> list = new ArrayList<>(value.size());
    for (Long long1 : value) {
      list.add(longToAnyValue(long1));
    }

    return list;
  }

  private static List<AnyValue> doubleListToAnyValue(List<Double> value) {
    List<AnyValue> list = new ArrayList<>(value.size());
    for (Double double1 : value) {
      list.add(doubleToAnyValue(double1));
    }

    return list;
  }

  private static List<String> anyValuesToStrings(List<AnyValue> values) {
    List<String> list = new ArrayList<>(values.size());
    for (AnyValue anyValue : values) {
      list.add(anyValueToString(anyValue));
    }

    return list;
  }

  private static List<Boolean> anyValuesToBooleans(List<AnyValue> values) {
    List<Boolean> list = new ArrayList<>(values.size());
    for (AnyValue anyValue : values) {
      list.add(anyValueToBoolean(anyValue));
    }

    return list;
  }

  private static List<Long> anyValuesToLongs(List<AnyValue> values) {
    List<Long> list = new ArrayList<>(values.size());
    for (AnyValue anyValue : values) {
      list.add(anyValueToLong(anyValue));
    }

    return list;
  }

  private static List<Double> anyValuesToDoubles(List<AnyValue> values) {
    List<Double> list = new ArrayList<>(values.size());
    for (AnyValue anyValue : values) {
      list.add(anyValueToDouble(anyValue));
    }

    return list;
  }

  private static String anyValueToString(AnyValue value) {
    return value.string_value;
  }

  private static Boolean anyValueToBoolean(AnyValue value) {
    return value.bool_value;
  }

  private static Long anyValueToLong(AnyValue value) {
    return value.int_value;
  }

  private static Double anyValueToDouble(AnyValue value) {
    return value.double_value;
  }
}
