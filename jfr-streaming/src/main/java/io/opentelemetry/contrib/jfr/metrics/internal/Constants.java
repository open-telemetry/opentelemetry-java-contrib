/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jfr.metrics.internal;

import io.opentelemetry.api.common.AttributeKey;

public final class Constants {
  private Constants() {}

  public static final String ONE = "1";
  public static final String KILOBYTES = "kb";
  public static final String MILLISECONDS = "ms";
  public static final String USER = "user";
  public static final String SYSTEM = "system";
  public static final String MACHINE = "machine.total";
  public static final String G1 = "g1";
  public static final String USED = "used";
  public static final String COMMITTED = "committed";

  public static final String NETWORK_BYTES_NAME = "runtime.jvm.network.io";
  public static final String NETWORK_BYTES_DESCRIPTION = "Network read/write bytes";
  public static final String NETWORK_DURATION_NAME = "runtime.jvm.network.duration";
  public static final String NETWORK_DURATION_DESCRIPTION = "Network read/write duration";
  public static final String NETWORK_MODE_READ = "read";
  public static final String NETWORK_MODE_WRITE = "write";

  public static final AttributeKey<String> ATTR_THREAD_NAME = AttributeKey.stringKey("thread.name");
  public static final AttributeKey<String> ATTR_ARENA_NAME = AttributeKey.stringKey("arena");
  public static final AttributeKey<String> ATTR_NETWORK_MODE = AttributeKey.stringKey("mode");
  public static final AttributeKey<String> ATTR_CPU_USAGE = AttributeKey.stringKey("usage.type");
  public static final AttributeKey<String> ATTR_GC_COLLECTOR = AttributeKey.stringKey("name");
  public static final AttributeKey<String> ATTR_MEMORY_USAGE = AttributeKey.stringKey("usage.type");

  public static final String METRIC_NAME_MEMORY_ALLOCATION = "runtime.jvm.memory.allocation";
}
