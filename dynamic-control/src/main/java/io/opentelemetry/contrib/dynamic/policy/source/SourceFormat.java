/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy.source;

import com.google.errorprone.annotations.Immutable;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Supported source formats and their parser dispatch. */
public enum SourceFormat {
  // TODO keyvalue is not fully implemented, ignore mappedPolicyIds until done
  KEYVALUE("keyvalue", (source, mappedPolicyIds) -> KeyValueSourceWrapper.parse(source)),
  JSONKEYVALUE("jsonkeyvalue", JsonSourceWrapper::parse);

  private final String configValue;
  private final SourceParser parser;

  SourceFormat(String configValue, SourceParser parser) {
    this.configValue = configValue;
    this.parser = parser;
  }

  public String configValue() {
    return configValue;
  }

  /**
   * Converts string value case insensitively to enum value. Leading and trailing whitespace is
   * removed, then the remainder is matched case-insensitively against {@link #configValue()} for
   * each format.
   *
   * @param value the string from config (e.g. {@code "keyvalue"}, {@code "JSONKEYVALUE"})
   * @return the matching format
   * @throws NullPointerException if value is null
   * @throws IllegalArgumentException if no format matches the trimmed value
   */
  public static SourceFormat fromConfigValue(@Nonnull String value) {
    Objects.requireNonNull(value, "value cannot be null");
    String normalized = value.trim().toLowerCase(Locale.ROOT);
    for (SourceFormat format : values()) {
      if (format.configValue.equals(normalized)) {
        return format;
      }
    }
    throw new IllegalArgumentException(
        "Unknown source format (normalized): '" + normalized + "' (original: '" + value + "')");
  }

  /**
   * Parses source text into normalized wrappers, using configured policy mappings when supported by
   * the format.
   *
   * @param source source text to parse
   * @param mappedPolicyIds configured policy IDs accepted by the parser
   * @return an empty list if the source is valid but contains no policies; a non-empty list of
   *     wrappers if one or more policies were parsed successfully; or {@code null} if the source is
   *     malformed or does not conform to the expected shape for this format
   * @throws NullPointerException if source or mappedPolicyIds is null
   */
  @Nullable
  public List<SourceWrapper> parse(String source, Set<String> mappedPolicyIds) {
    Objects.requireNonNull(source, "source cannot be null");
    Objects.requireNonNull(mappedPolicyIds, "mappedPolicyIds cannot be null");
    return parser.parse(source, mappedPolicyIds);
  }

  @Immutable
  @FunctionalInterface
  private interface SourceParser {
    @Nullable
    List<SourceWrapper> parse(String source, Set<String> mappedPolicyIds);
  }
}
