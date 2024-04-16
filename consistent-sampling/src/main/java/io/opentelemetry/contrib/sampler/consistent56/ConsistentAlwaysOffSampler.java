/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.sampler.consistent56;

import javax.annotation.concurrent.Immutable;

@Immutable
final class ConsistentAlwaysOffSampler extends ConsistentSampler {

  private static final ConsistentAlwaysOffSampler INSTANCE = new ConsistentAlwaysOffSampler();

  private ConsistentAlwaysOffSampler() {}

  static ConsistentAlwaysOffSampler getInstance() {
    return INSTANCE;
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
