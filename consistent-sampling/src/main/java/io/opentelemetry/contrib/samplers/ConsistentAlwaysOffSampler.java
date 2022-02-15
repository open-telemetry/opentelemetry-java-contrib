/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.samplers;

import io.opentelemetry.contrib.state.OtelTraceState;
import javax.annotation.concurrent.Immutable;

@Immutable
final class ConsistentAlwaysOffSampler extends ConsistentSampler {

  private ConsistentAlwaysOffSampler() {}

  private static final ConsistentSampler INSTANCE = new ConsistentAlwaysOffSampler();

  static ConsistentSampler getInstance() {
    return INSTANCE;
  }

  @Override
  protected int getP(int parentP, boolean isRoot) {
    return OtelTraceState.getMaxP();
  }

  @Override
  public String getDescription() {
    return "ConsistentAlwaysOffSampler";
  }
}
