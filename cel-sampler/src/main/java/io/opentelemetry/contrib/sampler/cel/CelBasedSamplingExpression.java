/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.sampler.cel;

import static java.util.Objects.requireNonNull;

import dev.cel.common.CelAbstractSyntaxTree;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import java.util.Objects;
import javax.annotation.Nullable;

/**
 * Represents a CEL-based sampling expression that contains a compiled CEL expression and its
 * associated sampler delegate.
 *
 * <p>This class is used internally by {@link CelBasedSampler} to store and evaluate CEL expressions
 * for sampling decisions.
 */
public final class CelBasedSamplingExpression {
  private final CelAbstractSyntaxTree abstractSyntaxTree;
  private final String expression;
  private final Sampler delegate;

  /**
   * Creates a new CEL-based sampling expression.
   *
   * @param abstractSyntaxTree The compiled CEL abstract syntax tree
   * @param delegate The sampler to use when this expression evaluates to true
   */
  CelBasedSamplingExpression(CelAbstractSyntaxTree abstractSyntaxTree, Sampler delegate) {
    this.abstractSyntaxTree =
        requireNonNull(abstractSyntaxTree, "abstractSyntaxTree must not be null");
    this.expression = abstractSyntaxTree.getSource().getContent().toString();
    this.delegate = requireNonNull(delegate, "delegate must not be null");
  }

  /**
   * Returns the compiled CEL abstract syntax tree.
   *
   * @return The abstract syntax tree
   */
  CelAbstractSyntaxTree getAbstractSyntaxTree() {
    return abstractSyntaxTree;
  }

  /**
   * Returns the string representation of the CEL expression.
   *
   * @return The expression string
   */
  String getExpression() {
    return expression;
  }

  /**
   * Returns the sampler delegate.
   *
   * @return The sampler delegate
   */
  Sampler getDelegate() {
    return delegate;
  }

  @Override
  public String toString() {
    return "CelBasedSamplingExpression{"
        + "expression='"
        + expression
        + "', delegate="
        + delegate
        + "}";
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof CelBasedSamplingExpression)) {
      return false;
    }
    CelBasedSamplingExpression that = (CelBasedSamplingExpression) o;
    return Objects.equals(abstractSyntaxTree, that.abstractSyntaxTree)
        && Objects.equals(expression, that.expression)
        && Objects.equals(delegate, that.delegate);
  }

  @Override
  public int hashCode() {
    return Objects.hash(abstractSyntaxTree, expression, delegate);
  }
}
