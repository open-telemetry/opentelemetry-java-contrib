/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.samplers;

import static java.util.Objects.requireNonNull;

import java.util.function.ToIntFunction;
import javax.annotation.concurrent.Immutable;

/**
 * A consistent sampler that makes the same sampling decision as the parent and optionally falls
 * back to an alternative consistent sampler, if the parent p-value is invalid (like for root
 * spans).
 */
@Immutable
final class ConsistentParentBasedSampler extends ConsistentSampler {

  private final ConsistentSampler rootSampler;

  private final String description;

  /**
   * Constructs a new consistent parent based sampler using the given root sampler and the given
   * thread-safe random generator.
   *
   * @param rootSampler the root sampler
   * @param rValueGenerator the function to use for generating the r-value
   */
  ConsistentParentBasedSampler(
      ConsistentSampler rootSampler, ToIntFunction<String> rValueGenerator) {
    super(rValueGenerator);
    this.rootSampler = requireNonNull(rootSampler);
    this.description =
        "ConsistentParentBasedSampler{rootSampler=" + rootSampler.getDescription() + '}';
  }

  @Override
  protected int getP(int parentP, boolean isRoot) {
    if (isRoot) {
      return rootSampler.getP(parentP, isRoot);
    } else {
      return parentP;
    }
  }

  @Override
  public String getDescription() {
    return description;
  }
}
