/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.inferredspans.internal.semconv;

import io.opentelemetry.api.common.AttributeKey;

public class Attributes {

  private Attributes() {}

  public static final AttributeKey<String> CODE_STACKTRACE =
      AttributeKey.stringKey("code.stacktrace");
  public static final AttributeKey<Boolean> LINK_IS_CHILD = AttributeKey.booleanKey("is_child");
  public static final AttributeKey<Boolean> SPAN_IS_INFERRED =
      AttributeKey.booleanKey("is_inferred");
}
