/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.sampler.cel;

import static java.util.Objects.requireNonNull;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import dev.cel.common.CelAbstractSyntaxTree;
import dev.cel.common.CelValidationException;
import dev.cel.compiler.CelCompiler;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import java.util.ArrayList;
import java.util.List;

/**
 * Builder for {@link CelBasedSampler}.
 *
 * <p>This builder allows configuring CEL expressions with their associated sampling actions. Each
 * expression is evaluated in order, and the first matching expression determines the sampling
 * decision for a span.
 */
public final class CelBasedSamplerBuilder {
  private final CelCompiler celCompiler;
  private final List<CelBasedSamplingExpression> expressions = new ArrayList<>();
  private final Sampler defaultDelegate;

  /**
   * Creates a new builder with the specified fallback sampler and CEL compiler.
   *
   * @param defaultDelegate The fallback sampler to use when no expressions match
   * @param celCompiler The CEL compiler for compiling expressions
   */
  CelBasedSamplerBuilder(Sampler defaultDelegate, CelCompiler celCompiler) {
    this.defaultDelegate = defaultDelegate;
    this.celCompiler = celCompiler;
  }

  /**
   * Use the provided sampler when the CEL expression evaluates to true.
   *
   * @param expression The CEL expression to evaluate
   * @param sampler The sampler to use when the expression matches
   * @return This builder instance for method chaining
   * @throws CelValidationException if the expression cannot be compiled
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
   * Drop all spans when the CEL expression evaluates to true.
   *
   * @param expression The CEL expression to evaluate
   * @return This builder instance for method chaining
   * @throws CelValidationException if the expression cannot be compiled
   */
  @CanIgnoreReturnValue
  public CelBasedSamplerBuilder drop(String expression) throws CelValidationException {
    return customize(expression, Sampler.alwaysOff());
  }

  /**
   * Record and sample all spans when the CEL expression evaluates to true.
   *
   * @param expression The CEL expression to evaluate
   * @return This builder instance for method chaining
   * @throws CelValidationException if the expression cannot be compiled
   */
  @CanIgnoreReturnValue
  public CelBasedSamplerBuilder recordAndSample(String expression) throws CelValidationException {
    return customize(expression, Sampler.alwaysOn());
  }

  /**
   * Build the sampler based on the configured expressions.
   *
   * @return a new {@link CelBasedSampler} instance
   */
  public CelBasedSampler build() {
    return new CelBasedSampler(expressions, defaultDelegate);
  }
}
