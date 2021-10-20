/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jfr.metrics.internal;

import io.opentelemetry.api.common.AttributeKey;

public final class Constants {
  private Constants() {}

  public static final String ONE = "1";
  public static final String KILOBYTES = "KB";
  public static final String MILLISECONDS = "ms";
  public static final String PERCENTAGE = "%age";
  public static final String READ = "read";
  public static final String WRITE = "write";
  public static final AttributeKey<String> ATTR_THREAD_NAME = AttributeKey.stringKey("thread.name");
  public static final AttributeKey<String> ATTR_ARENA_NAME = AttributeKey.stringKey("arena");
  public static final AttributeKey<String> ATTR_NETWORK_MODE = AttributeKey.stringKey("mode");
}
