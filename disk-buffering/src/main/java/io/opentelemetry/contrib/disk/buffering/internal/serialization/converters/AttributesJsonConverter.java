/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.serialization.converters;

import com.dslplatform.json.BoolConverter;
import com.dslplatform.json.JsonConverter;
import com.dslplatform.json.JsonReader;
import com.dslplatform.json.JsonWriter;
import com.dslplatform.json.NumberConverter;
import com.dslplatform.json.ObjectConverter;
import com.dslplatform.json.StringConverter;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.AttributeType;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

@SuppressWarnings({"rawtypes", "unchecked"})
@JsonConverter(target = Attributes.class)
public final class AttributesJsonConverter {
  private static final Map<AttributeType, String> simpleTypesInJson = new HashMap<>();
  private static final Map<String, AttributeType> jsonToSimpleTypes = new HashMap<>();
  private static final Map<AttributeType, AttributeType> arrayToSimpleTypes = new HashMap<>();

  private AttributesJsonConverter() {}

  static {
    simpleTypesInJson.put(AttributeType.BOOLEAN, "boolValue");
    simpleTypesInJson.put(AttributeType.DOUBLE, "doubleValue");
    simpleTypesInJson.put(AttributeType.LONG, "intValue");
    simpleTypesInJson.put(AttributeType.STRING, "stringValue");
    arrayToSimpleTypes.put(AttributeType.BOOLEAN_ARRAY, AttributeType.BOOLEAN);
    arrayToSimpleTypes.put(AttributeType.DOUBLE_ARRAY, AttributeType.DOUBLE);
    arrayToSimpleTypes.put(AttributeType.LONG_ARRAY, AttributeType.LONG);
    arrayToSimpleTypes.put(AttributeType.STRING_ARRAY, AttributeType.STRING);
    for (Map.Entry<AttributeType, String> entry : simpleTypesInJson.entrySet()) {
      jsonToSimpleTypes.put(entry.getValue(), entry.getKey());
    }
  }

  public static void write(JsonWriter writer, Attributes value) {
    Iterator<Map.Entry<AttributeKey<?>, Object>> entryIterator =
        value.asMap().entrySet().iterator();
    writer.writeAscii("[");
    while (entryIterator.hasNext()) {
      Map.Entry<AttributeKey<?>, Object> entry = entryIterator.next();
      writeItem(writer, entry.getKey(), entry.getValue(), entryIterator.hasNext());
    }
    writer.writeAscii("]");
  }

  public static Attributes read(JsonReader reader) throws IOException {
    AttributesBuilder builder = Attributes.builder();

    if (listIsEmpty(reader)) {
      return builder.build();
    }

    readObject(reader, builder);

    while (reader.getNextToken() != ']') {
      reader.startObject();
      readObject(reader, builder);
    }

    return builder.build();
  }

  private static void readObject(JsonReader reader, AttributesBuilder builder) throws IOException {
    Map<String, Object> map = ObjectConverter.deserializeMap(reader);
    String key = (String) map.get("key");
    Map<String, Object> value = (Map<String, Object>) map.get("value");
    putItem(
        builder,
        Objects.requireNonNull(key),
        Objects.requireNonNull(value).entrySet().iterator().next());
  }

  private static void putItem(
      AttributesBuilder builder, String key, Map.Entry<String, Object> entry) {
    Object value = entry.getValue();
    if (value instanceof Map) {
      List<Map<String, Object>> list =
          (List<Map<String, Object>>) ((Map<String, Object>) value).get("values");
      Objects.requireNonNull(list);
      AttributeType type = jsonToSimpleTypes.get(list.get(0).keySet().iterator().next());
      switch (Objects.requireNonNull(type)) {
        case BOOLEAN:
          builder.put(AttributeKey.booleanArrayKey(key), collectItems(list));
          break;
        case DOUBLE:
          builder.put(
              AttributeKey.doubleArrayKey(key),
              collectItems(list, item -> ((BigDecimal) item).doubleValue()));
          break;
        case LONG:
          builder.put(
              AttributeKey.longArrayKey(key),
              collectItems(list, item -> Long.parseLong((String) item)));
          break;
        case STRING:
          builder.put(AttributeKey.stringArrayKey(key), collectItems(list));
          break;
        default:
          throw new UnsupportedOperationException();
      }
    } else {
      switch (Objects.requireNonNull(jsonToSimpleTypes.get(entry.getKey()))) {
        case BOOLEAN:
          builder.put(AttributeKey.booleanKey(key), (Boolean) value);
          break;
        case DOUBLE:
          builder.put(AttributeKey.doubleKey(key), ((BigDecimal) value).doubleValue());
          break;
        case LONG:
          builder.put(AttributeKey.longKey(key), Long.parseLong((String) value));
          break;
        case STRING:
          builder.put(AttributeKey.stringKey(key), (String) value);
          break;
        default:
          throw new UnsupportedOperationException();
      }
    }
  }

  private static <T> List<T> collectItems(List<Map<String, Object>> list) {
    return collectItems(list, (item) -> (T) item);
  }

  private static <T> List<T> collectItems(
      List<Map<String, Object>> list, Function<Object, T> converter) {
    List<T> result = new ArrayList<>();
    for (Map<String, Object> map : list) {
      for (Object value : map.values()) {
        result.add(converter.apply(value));
      }
    }
    return result;
  }

  private static boolean listIsEmpty(JsonReader reader) throws IOException {
    if (reader.last() != '[') {
      throw reader.newParseError("Expecting '[' for list start");
    }
    byte nextToken = reader.getNextToken();
    return nextToken == ']';
  }

  private static void writeItem(
      JsonWriter writer, AttributeKey<?> attributeKey, Object object, boolean hasNext) {
    writer.writeAscii("{\"key\":");
    StringConverter.serialize(attributeKey.getKey(), writer);
    writer.writeAscii(",\"value\":{");
    writeValue(writer, attributeKey.getType(), object);
    writer.writeAscii("}");
    writer.writeAscii("}");
    if (hasNext) {
      writer.writeAscii(",");
    }
  }

  private static void writeValue(JsonWriter writer, AttributeType type, Object object) {
    if (arrayToSimpleTypes.containsKey(type)) {
      writeArrayValue(writer, type, object);
    } else {
      writeSimpleValue(writer, type, object);
    }
  }

  private static void writeSimpleValue(JsonWriter writer, AttributeType type, Object object) {
    writer.writeString(Objects.requireNonNull(simpleTypesInJson.get(type)));
    writer.writeAscii(":");
    switch (type) {
      case BOOLEAN:
        BoolConverter.serialize((Boolean) object, writer);
        break;
      case STRING:
        StringConverter.serialize((String) object, writer);
        break;
      case DOUBLE:
        NumberConverter.serialize((Double) object, writer);
        break;
      case LONG:
        StringConverter.serialize(String.valueOf(object), writer);
        break;
      default:
        throw new UnsupportedOperationException("Not supported type: " + type);
    }
  }

  private static void writeArrayValue(JsonWriter writer, AttributeType type, Object object) {
    Iterator<Object> iterator = ((Collection<Object>) object).iterator();
    AttributeType simpleType = Objects.requireNonNull(arrayToSimpleTypes.get(type));
    writer.writeAscii("\"arrayValue\":{");
    writer.writeAscii("\"values\":[");
    while (iterator.hasNext()) {
      writer.writeAscii("{");
      writeSimpleValue(writer, simpleType, iterator.next());
      writer.writeAscii("}");
      if (iterator.hasNext()) {
        writer.writeAscii(",");
      }
    }
    writer.writeAscii("]");
    writer.writeAscii("}");
  }
}
