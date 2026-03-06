/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy.source;

import com.google.errorprone.annotations.Immutable;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;

/** Supported source formats and their parser dispatch. */
public enum SourceFormat {
  KEYVALUE("keyvalue", KeyValueSourceWrapper::parse),
  JSON("json", JsonSourceWrapper::parse);

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
   * Parses source text into normalized wrappers for this format.
   *
   * @return an empty list if the source is valid but contains no policies; a non-empty list of
   *     wrappers if one or more policies were parsed successfully; or {@code null} if the source is
   *     malformed or does not conform to the expected shape for this format.
   * @throws NullPointerException if source is null
   */
  @Nullable
  public List<SourceWrapper> parse(String source) {
    Objects.requireNonNull(source, "source cannot be null");
    return parser.parse(source);
  }

  @Immutable
  @FunctionalInterface
  private interface SourceParser {
    @Nullable
    List<SourceWrapper> parse(String source);
  }
}
