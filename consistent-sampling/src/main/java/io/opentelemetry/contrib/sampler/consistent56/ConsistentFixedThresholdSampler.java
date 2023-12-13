/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.sampler.consistent56;

import static io.opentelemetry.contrib.sampler.consistent56.ConsistentSamplingUtil.calculateSamplingProbability;
import static io.opentelemetry.contrib.sampler.consistent56.ConsistentSamplingUtil.checkThreshold;

public class ConsistentFixedThresholdSampler extends ConsistentSampler {

  private final long threshold;
  private final String description;

  protected ConsistentFixedThresholdSampler(
      long threshold, RandomValueGenerator randomValueGenerator) {
    super(randomValueGenerator);
    checkThreshold(threshold);
    this.threshold = threshold;

    String thresholdString;
    if (threshold == ConsistentSamplingUtil.getMaxThreshold()) {
      thresholdString = "max";
    } else {
      thresholdString =
          ConsistentSamplingUtil.appendLast56BitHexEncodedWithoutTrailingZeros(
                  new StringBuilder(), threshold)
              .toString();
    }

    this.description =
        "ConsistentFixedThresholdSampler{threshold="
            + thresholdString
            + ", sampling probability="
            + calculateSamplingProbability(threshold)
            + "}";
  }

  @Override
  public String getDescription() {
    return description;
  }

  @Override
  protected long getThreshold(long parentThreshold, boolean isRoot) {
    return threshold;
  }
}
