/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy.source;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;

/** KEYVALUE-backed source wrapper for a single key/value policy entry. */
public final class KeyValueSourceWrapper implements SourceWrapper {
  private final String key;
  private final String value;

  public KeyValueSourceWrapper(String key, String value) {
    this.key = Objects.requireNonNull(key, "key cannot be null");
    this.value = Objects.requireNonNull(value, "value cannot be null");
  }

  @Override
  public SourceFormat getFormat() {
    return SourceFormat.KEYVALUE;
  }

  @Override
  @Nullable
  public String getPolicyType() {
    return key;
  }

  public String getKey() {
    return key;
  }

  public String getValue() {
    return value;
  }

  /**
   * Parses KEYVALUE source into one wrapper per non-empty line.
   *
   * @return an empty list if the source contains no non-blank lines; a non-empty list of wrappers
   *     if all non-blank lines are valid key=value pairs; or {@code null} if any line is malformed.
   * @throws NullPointerException if source is null
   */
  @Nullable
  public static List<SourceWrapper> parse(String source) {
    Objects.requireNonNull(source, "source cannot be null");
    String[] lines = source.split("\\R", -1);
    List<SourceWrapper> wrappers = new ArrayList<>();
    for (String rawLine : lines) {
      String trimmedLine = rawLine.trim();
      if (trimmedLine.isEmpty()) {
        continue;
      }
      KeyValueSourceWrapper wrapper = parseSingleKeyValue(trimmedLine);
      if (wrapper == null) {
        return null;
      }
      wrappers.add(wrapper);
    }
    if (wrappers.isEmpty()) {
      return Collections.emptyList();
    }
    return Collections.unmodifiableList(wrappers);
  }

  @Nullable
  private static KeyValueSourceWrapper parseSingleKeyValue(String line) {
    int separatorIndex = line.indexOf('=');
    if (separatorIndex <= 0) {
      return null;
    }
    String key = line.substring(0, separatorIndex).trim();
    String value = line.substring(separatorIndex + 1).trim();
    if (key.isEmpty()) {
      return null;
    }
    return new KeyValueSourceWrapper(key, value);
  }
}
