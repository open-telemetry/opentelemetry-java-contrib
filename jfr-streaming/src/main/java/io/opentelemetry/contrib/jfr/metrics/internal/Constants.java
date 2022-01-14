/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jfr.metrics.internal;

import io.opentelemetry.api.common.AttributeKey;

public final class Constants {
  private Constants() {}

  public static final String ONE = "1";
  public static final String HERTZ = "Hz";
  public static final String BYTES = "B";
  public static final String MILLISECONDS = "ms";
  public static final String PERCENTAGE = "%age";
  public static final String USER = "user";
  public static final String SYSTEM = "system";
  public static final String MACHINE = "machine.total";
  public static final String G1 = "g1";
  public static final String TOTAL_USED = "total.used";
  public static final String EDEN_USED = "eden.used";
  public static final String EDEN_SIZE = "eden.size";
  public static final String EDEN_SIZE_DELTA = "eden.delta";
  public static final String SURVIVOR_SIZE = "survivor.size";
  public static final String REGION_COUNT = "region.count";
  public static final String COMMITTED = "committed";
  public static final String RESERVED = "reserved";

  public static final String METRIC_NAME_NETWORK_BYTES = "runtime.jvm.network.io";
  public static final String METRIC_DESCRIPTION_NETWORK_BYTES = "Network read/write bytes";
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
  public static final String METRIC_DESCRIPTION_MEMORY_ALLOCATION = "Allocation";
}
