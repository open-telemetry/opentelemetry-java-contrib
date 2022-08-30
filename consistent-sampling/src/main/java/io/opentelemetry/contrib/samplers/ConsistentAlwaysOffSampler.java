/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.samplers;

import javax.annotation.concurrent.Immutable;

@Immutable
final class ConsistentAlwaysOffSampler extends ConsistentSampler {

  ConsistentAlwaysOffSampler(RValueGenerator rValueGenerator) {
    super(rValueGenerator);
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
