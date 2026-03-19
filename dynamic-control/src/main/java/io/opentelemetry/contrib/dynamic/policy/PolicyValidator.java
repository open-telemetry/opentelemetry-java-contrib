/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy;

import io.opentelemetry.contrib.dynamic.policy.source.JsonSourceWrapper;
import io.opentelemetry.contrib.dynamic.policy.source.KeyValueSourceWrapper;
import io.opentelemetry.contrib.dynamic.policy.source.SourceWrapper;
import javax.annotation.Nullable;

public interface PolicyValidator {
  /**
   * Validates a policy configuration provided as a JSON string.
   *
   * @param json The JSON string containing the policy configuration.
   * @return The validated {@link TelemetryPolicy}, or {@code null} if the JSON does not contain a
   *     valid policy for this validator.
   */
  @Nullable
  TelemetryPolicy validate(String json);

  /**
   * Validates a parsed policy configuration source.
   *
   * <p>This is a transitional API: by default it delegates to {@link #validate(String)} and/or
   * {@link #validateAlias(String, String)} where possible.
   *
   * @param source parsed source wrapper containing the format and payload
   * @return The validated {@link TelemetryPolicy}, or {@code null} if the source does not contain a
   *     valid policy for this validator.
   */
  @Nullable
  default TelemetryPolicy validate(SourceWrapper source) {
    if (source == null) {
      return null;
    }
    if (source instanceof JsonSourceWrapper) {
      return validate(((JsonSourceWrapper) source).asJsonNode().toString());
    }
    if (source instanceof KeyValueSourceWrapper) {
      KeyValueSourceWrapper kv = (KeyValueSourceWrapper) source;
      String alias = getAlias();
      if (alias != null && alias.equals(kv.getKey().trim())) {
        return validateAlias(kv.getKey().trim(), kv.getValue());
      }
    }
    return null;
  }

  /**
   * Returns the type of the policy this validator handles.
   *
   * @return The policy type string (e.g., "trace-sampling").
   */
  String getPolicyType();

  /**
   * Validates a policy configuration provided as a key-value pair (alias).
   *
   * <p>This is intended for simple configuration cases where a full JSON object is not necessary.
   *
   * @param key The alias key (e.g., "trace-sampling.probability").
   * @param value The value associated with the key.
   * @return The validated {@link TelemetryPolicy}, or {@code null} if the key/value pair is invalid
   *     or not handled by this validator.
   */
  @Nullable
  default TelemetryPolicy validateAlias(String key, String value) {
    throw new UnsupportedOperationException(
        "Alias validation is not supported by validator "
            + getClass().getName()
            + " for key "
            + key);
  }

  /**
   * Returns the alias key supported by this validator, if any.
   *
   * @return The alias key string, or {@code null} if aliases are not supported.
   */
  @Nullable
  default String getAlias() {
    return null;
  }
}
