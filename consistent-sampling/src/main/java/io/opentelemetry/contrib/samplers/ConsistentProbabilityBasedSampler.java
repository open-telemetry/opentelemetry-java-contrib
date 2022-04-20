/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.samplers;

import javax.annotation.concurrent.Immutable;

/** A consistent sampler that samples with a fixed probability. */
@Immutable
final class ConsistentProbabilityBasedSampler extends ConsistentSampler {

  private final int lowerPValue;
  private final int upperPValue;
  private final double probabilityToUseLowerPValue;
  private final String description;

  /**
   * Constructor.
   *
   * @param samplingProbability the sampling probability
   */
  ConsistentProbabilityBasedSampler(double samplingProbability) {
    this(samplingProbability, RandomGenerator.getDefault());
  }

  /**
   * Constructor.
   *
   * @param samplingProbability the sampling probability
   * @param randomGenerator a random generator
   */
  ConsistentProbabilityBasedSampler(double samplingProbability, RandomGenerator randomGenerator) {
    super(randomGenerator);
    if (samplingProbability < 0.0 || samplingProbability > 1.0) {
      throw new IllegalArgumentException("Sampling probability must be in range [0.0, 1.0]!");
    }
    this.description =
        String.format("ConsistentProbabilityBasedSampler{%.6f}", samplingProbability);

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
