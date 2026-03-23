/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy;

import io.opentelemetry.contrib.dynamic.policy.source.SourceWrapper;
import javax.annotation.Nullable;

public interface PolicyValidator {
  /**
   * Validates a parsed policy configuration source.
   *
   * @param source parsed source wrapper containing the format and payload
   * @return The validated {@link TelemetryPolicy}, or {@code null} if the source does not contain a
   *     valid policy for this validator.
   */
  @Nullable
  TelemetryPolicy validate(SourceWrapper source);

  /**
   * Returns the type of the policy this validator handles.
   *
   * @return The policy type string (e.g., "trace-sampling").
   */
  String getPolicyType();
}
