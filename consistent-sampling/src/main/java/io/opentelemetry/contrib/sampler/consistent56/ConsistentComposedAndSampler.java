/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.sampler.consistent56;

import static java.util.Objects.requireNonNull;

import javax.annotation.concurrent.Immutable;

/**
 * A consistent sampler composed of two consistent samplers.
 *
 * <p>This sampler samples if both samplers would sample.
 */
@Immutable
final class ConsistentComposedAndSampler extends ConsistentSampler {

  private final ConsistentSampler sampler1;
  private final ConsistentSampler sampler2;
  private final String description;

  ConsistentComposedAndSampler(
      ConsistentSampler sampler1,
      ConsistentSampler sampler2,
      RandomValueGenerator randomValueGenerator) {
    super(randomValueGenerator);
    this.sampler1 = requireNonNull(sampler1);
    this.sampler2 = requireNonNull(sampler2);
    this.description =
        "ConsistentComposedAndSampler{"
            + "sampler1="
            + sampler1.getDescription()
            + ",sampler2="
            + sampler2.getDescription()
            + '}';
  }

  @Override
  protected long getThreshold(long parentThreshold, boolean isRoot) {
    long threshold1 = sampler1.getThreshold(parentThreshold, isRoot);
    long threshold2 = sampler2.getThreshold(parentThreshold, isRoot);
    if (ConsistentSamplingUtil.isValidThreshold(threshold1)
        && ConsistentSamplingUtil.isValidThreshold(threshold2)) {
      return Math.min(threshold1, threshold2);
    } else {
      return ConsistentSamplingUtil.getInvalidThreshold();
    }
  }

  @Override
  public String getDescription() {
    return description;
  }
}
