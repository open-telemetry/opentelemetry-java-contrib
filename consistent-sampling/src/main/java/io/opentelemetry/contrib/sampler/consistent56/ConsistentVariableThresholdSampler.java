/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.sampler.consistent56;

import static io.opentelemetry.contrib.sampler.consistent56.ConsistentSamplingUtil.calculateSamplingProbability;
import static io.opentelemetry.contrib.sampler.consistent56.ConsistentSamplingUtil.calculateThreshold;
import static io.opentelemetry.contrib.sampler.consistent56.ConsistentSamplingUtil.checkThreshold;
import static io.opentelemetry.contrib.sampler.consistent56.ConsistentSamplingUtil.getMaxThreshold;

public class ConsistentVariableThresholdSampler extends ConsistentThresholdSampler {

  private volatile long threshold;
  private volatile String description = "";

  protected ConsistentVariableThresholdSampler(double samplingProbability) {
    setSamplingProbability(samplingProbability);
  }

  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public long getThreshold() {
    return threshold;
  }

  public void setSamplingProbability(double samplingProbability) {
    long threshold = calculateThreshold(samplingProbability);
    checkThreshold(threshold);
    this.threshold = threshold;

    String thresholdString;
    if (threshold == getMaxThreshold()) {
      thresholdString = "max";
    } else {
      thresholdString =
          ConsistentSamplingUtil.appendLast56BitHexEncodedWithoutTrailingZeros(
                  new StringBuilder(), threshold)
              .toString();
    }

    // tiny eventual consistency where the description would be out of date with the threshold,
    // but this doesn't really matter
    this.description =
        "ConsistentVariableThresholdSampler{threshold="
            + thresholdString
            + ", sampling probability="
            + calculateSamplingProbability(threshold)
            + "}";
  }
}
