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
 * <p>This sampler samples if any of the two samplers would sample.
 */
@Immutable
final class ConsistentComposedOrSampler extends ConsistentSampler {

  private final ConsistentSampler sampler1;
  private final ConsistentSampler sampler2;
  private final String description;

  ConsistentComposedOrSampler(ConsistentSampler sampler1, ConsistentSampler sampler2) {
    this.sampler1 = requireNonNull(sampler1);
    this.sampler2 = requireNonNull(sampler2);
    this.description =
        "ConsistentComposedOrSampler{"
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
    if (ConsistentSamplingUtil.isValidThreshold(threshold1)) {
      if (ConsistentSamplingUtil.isValidThreshold(threshold2)) {
        return Math.min(threshold1, threshold2);
      }
      return threshold1;
    } else {
      if (ConsistentSamplingUtil.isValidThreshold(threshold2)) {
        return threshold2;
      }
      return ConsistentSamplingUtil.getInvalidThreshold();
    }
  }

  @Override
  public String getDescription() {
    return description;
  }
}
