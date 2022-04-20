/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.samplers;

import javax.annotation.concurrent.Immutable;

@Immutable
final class ConsistentAlwaysOnSampler extends ConsistentSampler {

  private ConsistentAlwaysOnSampler() {}

  private static final ConsistentSampler INSTANCE = new ConsistentAlwaysOnSampler();

  static ConsistentSampler getInstance() {
    return INSTANCE;
  }

  @Override
  protected int getP(int parentP, boolean isRoot) {
    return 0;
  }

  @Override
  public String getDescription() {
    return "ConsistentAlwaysOnSampler";
  }
}
