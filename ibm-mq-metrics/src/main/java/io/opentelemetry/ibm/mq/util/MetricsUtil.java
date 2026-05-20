/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.ibm.mq.util;

import java.util.function.Function;

public final class MetricsUtil {

  private MetricsUtil() {}

  public static final Function<Integer, Long> MIBY_TO_BYTES = x -> x * 1024L * 1024L;
}
