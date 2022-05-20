/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.metrics.micrometer.internal.instruments;

import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

final class RandomUtils {

  @SuppressWarnings("StreamToIterable")
  static Iterable<Double> randomDoubles(long size, double origin, double bound) {
    Stream<Double> stream = ThreadLocalRandom.current().doubles(size, origin, bound).boxed();

    return stream::iterator;
  }

  @SuppressWarnings("StreamToIterable")
  static Iterable<Long> randomLongs(long size, long origin, long bound) {
    Stream<Long> stream = ThreadLocalRandom.current().longs(size, origin, bound).boxed();

    return stream::iterator;
  }

  private RandomUtils() {}
}
