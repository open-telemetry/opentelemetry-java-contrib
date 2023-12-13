/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.sampler.consistent56;

import static io.opentelemetry.contrib.sampler.consistent56.ConsistentSamplingUtil.getMinThreshold;

import javax.annotation.concurrent.Immutable;

@Immutable
final class ConsistentAlwaysOnSampler extends ConsistentSampler {

  ConsistentAlwaysOnSampler(RandomValueGenerator randomValueGenerator) {
    super(randomValueGenerator);
  }

  @Override
  protected long getThreshold(long parentThreshold, boolean isRoot) {
    return getMinThreshold();
  }

  @Override
  public String getDescription() {
    return "ConsistentAlwaysOnSampler";
  }
}
