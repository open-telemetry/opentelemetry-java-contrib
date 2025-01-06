/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.sampler.consistent56;

import static java.util.Objects.requireNonNull;

/** A class for holding a pair (Predicate, Composable) */
public final class PredicatedSampler {

  public static PredicatedSampler onMatch(Predicate predicate, Composable sampler) {
    return new PredicatedSampler(predicate, sampler);
  }

  private final Predicate predicate;
  private final Composable sampler;

  private PredicatedSampler(Predicate predicate, Composable sampler) {
    this.predicate = requireNonNull(predicate);
    this.sampler = requireNonNull(sampler);
  }

  public Predicate getPredicate() {
    return predicate;
  }

  public Composable getSampler() {
    return sampler;
  }
}
