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
import java.util.Random;
import javax.annotation.concurrent.Immutable;

/**
 * A consistent sampler that delegates the decision randomly to one of its two delegates. Used by
 * unit tests.
 */
@Immutable
final class CoinFlipSampler extends ConsistentSampler {

  private static final Random random = new Random(System.currentTimeMillis());

  private final ComposableSampler samplerA;
  private final ComposableSampler samplerB;

  private final String description;

  /**
   * Constructs a new consistent CoinFlipSampler using the given two delegates.
   *
   * @param samplerA the first delegate sampler
   * @param samplerB the second delegate sampler
   */
  CoinFlipSampler(ComposableSampler samplerA, ComposableSampler samplerB) {
    this.samplerA = requireNonNull(samplerA);
    this.samplerB = requireNonNull(samplerB);
    this.description =
        "CoinFlipSampler{samplerA="
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

    if (random.nextDouble() <= 0.5) {
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
