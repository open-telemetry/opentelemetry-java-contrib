/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.sampler;

import static java.util.Objects.requireNonNull;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import dev.cel.common.CelAbstractSyntaxTree;
import dev.cel.common.CelValidationException;
import dev.cel.compiler.CelCompiler;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import java.util.ArrayList;
import java.util.List;

public final class CelBasedSamplerBuilder {
  private final CelCompiler celCompiler;
  private final List<CelBasedSamplingExpression> expressions = new ArrayList<>();
  private final Sampler defaultDelegate;

  CelBasedSamplerBuilder(Sampler defaultDelegate, CelCompiler celCompiler) {
    this.defaultDelegate = defaultDelegate;
    this.celCompiler = celCompiler;
  }

  /**
   * Use the provided sampler when the value of the provided {@link AttributeKey} matches the
   * provided pattern.
   */
  @CanIgnoreReturnValue
  public CelBasedSamplerBuilder customize(String expression, Sampler sampler)
      throws CelValidationException {
    CelAbstractSyntaxTree abstractSyntaxTree =
        celCompiler.compile(requireNonNull(expression, "expression must not be null")).getAst();

    expressions.add(
        new CelBasedSamplingExpression(
            requireNonNull(abstractSyntaxTree, "abstractSyntaxTree must not be null"),
            requireNonNull(sampler, "sampler must not be null")));
    return this;
  }

  /**
   * Drop all spans when the value of the provided {@link AttributeKey} matches the provided
   * pattern.
   */
  @CanIgnoreReturnValue
  public CelBasedSamplerBuilder drop(String expression) throws CelValidationException {
    return customize(expression, Sampler.alwaysOff());
  }

  /**
   * Record and sample all spans when the value of the provided {@link AttributeKey} matches the
   * provided pattern.
   */
  @CanIgnoreReturnValue
  public CelBasedSamplerBuilder recordAndSample(String expression) throws CelValidationException {
    return customize(expression, Sampler.alwaysOn());
  }

  /** Build the sampler based on the rules provided. */
  public CelBasedSampler build() {
    return new CelBasedSampler(expressions, defaultDelegate);
  }
}
