/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.sampler.consistent;

final class RValueGenerators {

  private static final RValueGenerator DEFAULT = createDefault();

  static RValueGenerator getDefault() {
    return DEFAULT;
  }

  private static RValueGenerator createDefault() {
    RandomGenerator randomGenerator = RandomGenerator.getDefault();
    return s -> randomGenerator.numberOfLeadingZerosOfRandomLong();
  }

  private RValueGenerators() {}
}
