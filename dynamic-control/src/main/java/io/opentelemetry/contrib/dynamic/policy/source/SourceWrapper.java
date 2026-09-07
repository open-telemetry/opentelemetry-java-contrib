/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy.source;

import javax.annotation.Nullable;

/** Parsed source payload paired with its source format. */
public interface SourceWrapper {
  SourceFormat getFormat();

  @Nullable
  String getPolicyType();

  /**
   * Returns a copy of this source with its policy identifier replaced by {@code policyType}.
   *
   * @return the remapped source, or {@code null} if this source cannot be remapped
   */
  @Nullable
  SourceWrapper withPolicyType(String policyType);
}
