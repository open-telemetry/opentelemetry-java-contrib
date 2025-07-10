/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.messaging.wrappers;

import io.opentelemetry.context.propagation.TextMapGetter;
import java.util.Collections;
import javax.annotation.Nullable;

public class NoopTextMapGetter<REQUEST> implements TextMapGetter<REQUEST> {

  public static <REQUEST> TextMapGetter<REQUEST> create() {
    return new NoopTextMapGetter<>();
  }

  @Override
  public Iterable<String> keys(REQUEST request) {
    return Collections.emptyList();
  }

  @Nullable
  @Override
  public String get(@Nullable REQUEST request, String s) {
    return null;
  }

  private NoopTextMapGetter() {}
}
