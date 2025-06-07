/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.sampler;

import static java.util.Objects.requireNonNull;

import dev.cel.common.CelAbstractSyntaxTree;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import java.util.Objects;
import javax.annotation.Nullable;

public class CelBasedSamplingExpression {
  final CelAbstractSyntaxTree abstractSyntaxTree;
  final String expression;
  final Sampler delegate;

  CelBasedSamplingExpression(CelAbstractSyntaxTree abstractSyntaxTree, Sampler delegate) {
    this.abstractSyntaxTree = requireNonNull(abstractSyntaxTree);
    this.expression = abstractSyntaxTree.getSource().getContent().toString();
    this.delegate = requireNonNull(delegate);
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
