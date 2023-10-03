/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.sampler.consistent2;

import static io.opentelemetry.contrib.sampler.consistent2.ConsistentSamplingUtil.getInvalidThreshold;
import static java.util.Objects.requireNonNull;

import javax.annotation.concurrent.Immutable;

/**
 * A consistent sampler that makes the same sampling decision as the parent. For root spans the
 * sampling decision is delegated to the root sampler.
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
   * @param randomValueGenerator the function to use for generating the r-value
   */
  ConsistentParentBasedSampler(
      ConsistentSampler rootSampler, RandomValueGenerator randomValueGenerator) {
    super(randomValueGenerator);
    this.rootSampler = requireNonNull(rootSampler);
    this.description =
        "ConsistentParentBasedSampler{rootSampler=" + rootSampler.getDescription() + '}';
  }

  @Override
  protected long getThreshold(long parentThreshold, boolean isRoot) {
    if (isRoot) {
      return rootSampler.getThreshold(getInvalidThreshold(), isRoot);
    } else {
      return parentThreshold;
    }
  }

  @Override
  public String getDescription() {
    return description;
  }
}
