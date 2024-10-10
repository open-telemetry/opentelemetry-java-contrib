/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.sampler.consistent56;

import static java.util.Objects.requireNonNull;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.data.LinkData;
import java.util.List;
import java.util.SplittableRandom;
import javax.annotation.concurrent.Immutable;

/**
 * A consistent sampler that delegates the decision randomly, with a predefined probability, to one
 * of its two delegates. Used by unit tests.
 */
@Immutable
final class CoinFlipSampler extends ConsistentSampler {

  private static final SplittableRandom random = new SplittableRandom(0x160a50a2073e17e6L);

  private final ComposableSampler samplerA;
  private final ComposableSampler samplerB;
  private final double probability;
  private final String description;

  /**
   * Constructs a new consistent CoinFlipSampler using the given two delegates with equal
   * probability.
   *
   * @param samplerA the first delegate sampler
   * @param samplerB the second delegate sampler
   */
  CoinFlipSampler(ComposableSampler samplerA, ComposableSampler samplerB) {
    this(samplerA, samplerB, 0.5);
  }

  /**
   * Constructs a new consistent CoinFlipSampler using the given two delegates, and the probability
   * to use the first one.
   *
   * @param probability the probability to use the first sampler
   * @param samplerA the first delegate sampler
   * @param samplerB the second delegate sampler
   */
  CoinFlipSampler(ComposableSampler samplerA, ComposableSampler samplerB, double probability) {
    this.samplerA = requireNonNull(samplerA);
    this.samplerB = requireNonNull(samplerB);
    this.probability = probability;
    this.description =
        "CoinFlipSampler{p="
            + (float) probability
            + ",samplerA="
            + samplerA.getDescription()
            + ','
            + "samplerB="
            + samplerB.getDescription()
            + '}';
  }

  @Override
  public SamplingIntent getSamplingIntent(
      Context parentContext,
      String name,
      SpanKind spanKind,
      Attributes attributes,
      List<LinkData> parentLinks) {

    if (random.nextDouble() < probability) {
      return samplerA.getSamplingIntent(parentContext, name, spanKind, attributes, parentLinks);
    } else {
      return samplerB.getSamplingIntent(parentContext, name, spanKind, attributes, parentLinks);
    }
  }

  @Override
  public String getDescription() {
    return description;
  }
}
