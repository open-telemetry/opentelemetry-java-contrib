/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.sampler;

import dev.cel.common.CelAbstractSyntaxTree;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import java.util.Objects;
import javax.annotation.Nullable;

public class CelBasedSamplingExpression {
  final Sampler delegate;
  final String expression;
  final CelAbstractSyntaxTree abstractSyntaxTree;

  CelBasedSamplingExpression(
      Sampler delegate, String expression, CelAbstractSyntaxTree abstractSyntaxTree) {
    this.delegate = delegate;
    this.expression = expression;
    this.abstractSyntaxTree = abstractSyntaxTree;
  }

  @Override
  public String toString() {
    return "CelBasedSamplingExpression{"
        + "delegate="
        + delegate
        + ", expression='"
        + expression
        + '}';
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
    return Objects.equals(delegate, that.delegate)
        && Objects.equals(expression, that.expression)
        && Objects.equals(abstractSyntaxTree, that.abstractSyntaxTree);
  }

  @Override
  public int hashCode() {
    return Objects.hash(delegate, expression, abstractSyntaxTree);
  }
}
