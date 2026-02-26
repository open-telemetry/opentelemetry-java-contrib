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
}
