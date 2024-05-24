/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.sampler;

import io.opentelemetry.api.common.AttributeKey;

class IncubatingAttributes {

  private IncubatingAttributes() {}

  public static final AttributeKey<String> THREAD_NAME = AttributeKey.stringKey("thread.name");
}
