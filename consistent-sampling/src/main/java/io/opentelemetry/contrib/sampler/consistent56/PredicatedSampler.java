/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.sampler.consistent56;

import static java.util.Objects.requireNonNull;

/** A class for holding a pair (Predicate, ComposableSampler) */
public final class PredicatedSampler {

  public static PredicatedSampler onMatch(Predicate predicate, ComposableSampler sampler) {
    return new PredicatedSampler(predicate, sampler);
  }

  private final Predicate predicate;
  private final ComposableSampler sampler;

  private PredicatedSampler(Predicate predicate, ComposableSampler sampler) {
    this.predicate = requireNonNull(predicate);
    this.sampler = requireNonNull(sampler);
  }

  public Predicate getPredicate() {
    return predicate;
  }

  public ComposableSampler getSampler() {
    return sampler;
  }
}
