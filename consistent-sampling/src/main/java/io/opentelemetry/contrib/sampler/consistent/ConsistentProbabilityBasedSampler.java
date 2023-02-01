/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.sampler.consistent;

import javax.annotation.concurrent.Immutable;

/** A consistent sampler that samples with a fixed probability. */
@Immutable
final class ConsistentProbabilityBasedSampler extends ConsistentSampler {

  private final int lowerPValue;
  private final int upperPValue;
  private final double probabilityToUseLowerPValue;
  private final String description;
  private final RandomGenerator randomGenerator;

  /**
   * Constructor.
   *
   * @param samplingProbability the sampling probability
   * @param rValueGenerator the function to use for generating the r-value
   */
  ConsistentProbabilityBasedSampler(
      double samplingProbability,
      RValueGenerator rValueGenerator,
      RandomGenerator randomGenerator) {
    super(rValueGenerator);
    if (samplingProbability < 0.0 || samplingProbability > 1.0) {
      throw new IllegalArgumentException("Sampling probability must be in range [0.0, 1.0]!");
    }
    this.description =
        String.format("ConsistentProbabilityBasedSampler{%.6f}", samplingProbability);
    this.randomGenerator = randomGenerator;

    lowerPValue = getLowerBoundP(samplingProbability);
    upperPValue = getUpperBoundP(samplingProbability);

    if (lowerPValue == upperPValue) {
      probabilityToUseLowerPValue = 1;
    } else {
      double upperSamplingProbability = getSamplingProbability(lowerPValue);
      double lowerSamplingProbability = getSamplingProbability(upperPValue);
      probabilityToUseLowerPValue =
          (samplingProbability - lowerSamplingProbability)
              / (upperSamplingProbability - lowerSamplingProbability);
    }
  }

  @Override
  protected int getP(int parentP, boolean isRoot) {
    if (randomGenerator.nextBoolean(probabilityToUseLowerPValue)) {
      return lowerPValue;
    } else {
      return upperPValue;
    }
  }

  @Override
  public String getDescription() {
    return description;
  }
}
