/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.maven;

import io.opentelemetry.context.propagation.TextMapGetter;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

final class ToUpperCaseTextMapGetter implements TextMapGetter<Map<String, String>> {
  @Override
  public Set<String> keys(Map<String, String> environmentVariables) {
    return environmentVariables.keySet();
  }

  @Override
  @Nullable
  public String get(@Nullable Map<String, String> environmentVariables, @Nonnull String key) {
    return environmentVariables == null
        ? null
        : environmentVariables.get(key.toUpperCase(Locale.ROOT));
  }
}
