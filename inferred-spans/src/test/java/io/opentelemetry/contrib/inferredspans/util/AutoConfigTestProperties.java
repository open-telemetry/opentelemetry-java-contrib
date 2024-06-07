/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.inferredspans.util;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import javax.annotation.Nullable;

public class AutoConfigTestProperties extends TemporaryProperties {

  public AutoConfigTestProperties() {
    put("otel.java.global-autoconfigure.enabled", "true");
    put("otel.traces.exporter", "logging");
    put("otel.metrics.exporter", "logging");
    put("otel.logs.exporter", "logging");
  }

  @Override
  @CanIgnoreReturnValue
  public AutoConfigTestProperties put(String key, @Nullable String value) {
    super.put(key, value);
    return this;
  }
}
