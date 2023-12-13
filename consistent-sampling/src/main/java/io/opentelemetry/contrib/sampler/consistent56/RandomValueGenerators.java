/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.sampler.consistent56;

import static io.opentelemetry.contrib.sampler.consistent56.ConsistentSamplingUtil.getMaxRandomValue;

import java.util.concurrent.ThreadLocalRandom;

final class RandomValueGenerators {

  private static final RandomValueGenerator DEFAULT = createDefault();

  static RandomValueGenerator getDefault() {
    return DEFAULT;
  }

  private static RandomValueGenerator createDefault() {
    return s -> ThreadLocalRandom.current().nextLong() & getMaxRandomValue();
  }

  private RandomValueGenerators() {}
}
