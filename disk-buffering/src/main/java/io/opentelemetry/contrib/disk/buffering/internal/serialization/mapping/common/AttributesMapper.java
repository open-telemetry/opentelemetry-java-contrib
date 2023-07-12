/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.common;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.AttributeType;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.common.v1.ArrayValue;
import io.opentelemetry.proto.common.v1.KeyValue;
import java.util.ArrayList;
import java.util.List;

public final class AttributesMapper {

  private static final AttributesMapper INSTANCE = new AttributesMapper();

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
      addValue(builder, keyValue.getKey(), keyValue.getValue());
    }
    return builder.build();
  }

  private static KeyValue attributeEntryToProto(AttributeKey<?> key, Object value) {
    KeyValue.Builder builder = KeyValue.newBuilder();
    builder.setKey(key.getKey());
    builder.setValue(attributeValueToProto(key.getType(), value));
    return builder.build();
  }

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
    }
    throw new UnsupportedOperationException();
  }

  private static AnyValue arrayToAnyValue(List<AnyValue> value) {
    return AnyValue.newBuilder()
        .setArrayValue(ArrayValue.newBuilder().addAllValues(value).build())
        .build();
  }

  private static void addValue(AttributesBuilder builder, String key, AnyValue value) {
    if (value.hasStringValue()) {
      builder.put(AttributeKey.stringKey(key), value.getStringValue());
    } else if (value.hasBoolValue()) {
      builder.put(AttributeKey.booleanKey(key), value.getBoolValue());
    } else if (value.hasIntValue()) {
      builder.put(AttributeKey.longKey(key), value.getIntValue());
    } else if (value.hasDoubleValue()) {
      builder.put(AttributeKey.doubleKey(key), value.getDoubleValue());
    } else if (value.hasArrayValue()) {
      addArray(builder, key, value.getArrayValue());
    } else {
      throw new UnsupportedOperationException();
    }
  }

  private static void addArray(AttributesBuilder builder, String key, ArrayValue arrayValue) {
    List<AnyValue> values = arrayValue.getValuesList();
    AnyValue anyValue = values.get(0);
    if (anyValue.hasStringValue()) {
      builder.put(AttributeKey.stringArrayKey(key), anyValuesToStrings(values));
    } else if (anyValue.hasBoolValue()) {
      builder.put(AttributeKey.booleanArrayKey(key), anyValuesToBooleans(values));
    } else if (anyValue.hasIntValue()) {
      builder.put(AttributeKey.longArrayKey(key), anyValuesToLongs(values));
    } else if (anyValue.hasDoubleValue()) {
      builder.put(AttributeKey.doubleArrayKey(key), anyValuesToDoubles(values));
    } else {
      throw new UnsupportedOperationException();
    }
  }

  private static AnyValue stringToAnyValue(String value) {
    AnyValue.Builder anyValue = AnyValue.newBuilder();

    anyValue.setStringValue(value);

    return anyValue.build();
  }

  private static AnyValue booleanToAnyValue(Boolean value) {
    AnyValue.Builder anyValue = AnyValue.newBuilder();

    if (value != null) {
      anyValue.setBoolValue(value);
    }

    return anyValue.build();
  }

  private static AnyValue longToAnyValue(Long value) {
    AnyValue.Builder anyValue = AnyValue.newBuilder();

    if (value != null) {
      anyValue.setIntValue(value);
    }

    return anyValue.build();
  }

  private static AnyValue doubleToAnyValue(Double value) {
    AnyValue.Builder anyValue = AnyValue.newBuilder();

    if (value != null) {
      anyValue.setDoubleValue(value);
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
    return value.getStringValue();
  }

  private static Boolean anyValueToBoolean(AnyValue value) {
    return value.getBoolValue();
  }

  private static Long anyValueToLong(AnyValue value) {
    return value.getIntValue();
  }

  private static Double anyValueToDouble(AnyValue value) {
    return value.getDoubleValue();
  }
}
