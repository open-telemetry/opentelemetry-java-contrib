/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy;

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
