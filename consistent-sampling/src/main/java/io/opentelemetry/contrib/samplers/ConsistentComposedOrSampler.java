/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.samplers;

import static java.util.Objects.requireNonNull;

import io.opentelemetry.contrib.state.OtelTraceState;
import javax.annotation.concurrent.Immutable;

/**
 * A consistent sampler composes two consistent samplers.
 *
 * <p>This sampler samples if any of the two samplers would sample.
 */
@Immutable
public final class ConsistentComposedOrSampler extends ConsistentSampler {

  private final ConsistentSampler sampler1;
  private final ConsistentSampler sampler2;
  private final String description;

  public static ConsistentComposedOrSampler create(
      ConsistentSampler sampler1, ConsistentSampler sampler2) {
    return new ConsistentComposedOrSampler(sampler1, sampler2);
  }

  private ConsistentComposedOrSampler(ConsistentSampler sampler1, ConsistentSampler sampler2) {
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
  protected int getP(int parentP, boolean isRoot) {
    int p1 = sampler1.getP(parentP, isRoot);
    int p2 = sampler2.getP(parentP, isRoot);
    if (OtelTraceState.isValidP(p1)) {
      if (OtelTraceState.isValidP(p2)) {
        return Math.min(p1, p2);
      } else {
        return p1;
      }
    } else {
      if (OtelTraceState.isValidP(p2)) {
        return p2;
      } else {
        return OtelTraceState.getInvalidP();
      }
    }
  }

  @Override
  public String getDescription() {
    return description;
  }
}
