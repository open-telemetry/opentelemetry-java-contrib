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
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public abstract class AttributesMapper {

  public static final AttributesMapper INSTANCE = new AttributesMapperImpl();

  public List<KeyValue> attributesToProto(Attributes attributes) {
    List<KeyValue> keyValues = new ArrayList<>();
    attributes.forEach((attributeKey, o) -> keyValues.add(attributeEntryToProto(attributeKey, o)));
    return keyValues;
  }

  protected KeyValue attributeEntryToProto(AttributeKey<?> key, Object value) {
    KeyValue.Builder builder = KeyValue.newBuilder();
    builder.setKey(key.getKey());
    builder.setValue(attributeValueToProto(key.getType(), value));
    return builder.build();
  }

  @SuppressWarnings("unchecked")
  private AnyValue attributeValueToProto(AttributeType type, Object value) {
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

  @Mapping(target = "stringValue", source = ".")
  protected abstract AnyValue stringToAnyValue(String value);

  @Mapping(target = "boolValue", source = ".")
  protected abstract AnyValue booleanToAnyValue(Boolean value);

  @Mapping(target = "intValue", source = ".")
  protected abstract AnyValue longToAnyValue(Long value);

  @Mapping(target = "doubleValue", source = ".")
  protected abstract AnyValue doubleToAnyValue(Double value);

  protected abstract List<AnyValue> stringListToAnyValue(List<String> value);

  protected abstract List<AnyValue> booleanListToAnyValue(List<Boolean> value);

  protected abstract List<AnyValue> longListToAnyValue(List<Long> value);

  protected abstract List<AnyValue> doubleListToAnyValue(List<Double> value);

  private static AnyValue arrayToAnyValue(List<AnyValue> value) {
    return AnyValue.newBuilder()
        .setArrayValue(ArrayValue.newBuilder().addAllValues(value).build())
        .build();
  }

  // FROM PROTO

  public Attributes protoToAttributes(List<KeyValue> values) {
    AttributesBuilder builder = Attributes.builder();
    for (KeyValue keyValue : values) {
      addValue(builder, keyValue.getKey(), keyValue.getValue());
    }
    return builder.build();
  }

  private void addValue(AttributesBuilder builder, String key, AnyValue value) {
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

  private void addArray(AttributesBuilder builder, String key, ArrayValue arrayValue) {
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

  protected abstract List<String> anyValuesToStrings(List<AnyValue> values);

  protected abstract List<Boolean> anyValuesToBooleans(List<AnyValue> values);

  protected abstract List<Long> anyValuesToLongs(List<AnyValue> values);

  protected abstract List<Double> anyValuesToDoubles(List<AnyValue> values);

  protected String anyValueToString(AnyValue value) {
    return value.getStringValue();
  }

  protected Boolean anyValueToBoolean(AnyValue value) {
    return value.getBoolValue();
  }

  protected Long anyValueToLong(AnyValue value) {
    return value.getIntValue();
  }

  protected Double anyValueToDouble(AnyValue value) {
    return value.getDoubleValue();
  }
}
