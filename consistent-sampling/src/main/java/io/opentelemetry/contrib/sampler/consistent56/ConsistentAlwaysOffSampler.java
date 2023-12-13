/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.sampler.consistent56;

import javax.annotation.concurrent.Immutable;

@Immutable
final class ConsistentAlwaysOffSampler extends ConsistentSampler {

  ConsistentAlwaysOffSampler(RandomValueGenerator randomValueGenerator) {
    super(randomValueGenerator);
  }

  @Override
  protected long getThreshold(long parentThreshold, boolean isRoot) {
    return ConsistentSamplingUtil.getMaxThreshold();
  }

  @Override
  public String getDescription() {
    return "ConsistentAlwaysOffSampler";
  }
}
