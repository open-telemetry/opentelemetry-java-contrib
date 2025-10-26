/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.sampler.cel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import dev.cel.common.CelAbstractSyntaxTree;
import dev.cel.common.CelValidationException;
import dev.cel.compiler.CelCompilerFactory;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import org.junit.jupiter.api.Test;

final class CelBasedSamplingExpressionTest {

  @Test
  void testThatThrowsOnNullParameter() throws CelValidationException {
    CelAbstractSyntaxTree ast =
        CelCompilerFactory.standardCelCompilerBuilder().build().compile("1 == 1").getAst();
    assertThatExceptionOfType(NullPointerException.class)
        .isThrownBy(() -> new CelBasedSamplingExpression(ast, null));

    assertThatExceptionOfType(NullPointerException.class)
        .isThrownBy(() -> new CelBasedSamplingExpression(null, Sampler.alwaysOn()));
  }

  @Test
  void testToString() throws CelValidationException {
    CelAbstractSyntaxTree ast =
        CelCompilerFactory.standardCelCompilerBuilder().build().compile("1 == 1").getAst();
    CelBasedSamplingExpression celExpression =
        new CelBasedSamplingExpression(ast, Sampler.alwaysOn());
    String expected = "CelBasedSamplingExpression{expression='1 == 1', delegate=AlwaysOnSampler}";
    assertThat(celExpression.toString()).isEqualTo(expected);
  }

  @Test
  void testEquals() throws CelValidationException {
    CelBasedSamplingExpression celExpressionOneEqualsOne1 =
        new CelBasedSamplingExpression(
            CelCompilerFactory.standardCelCompilerBuilder().build().compile("1 == 1").getAst(),
            Sampler.alwaysOn());

    assertThat(celExpressionOneEqualsOne1).isEqualTo(celExpressionOneEqualsOne1);
    assertThat(celExpressionOneEqualsOne1).isNotEqualTo(null);

    CelBasedSamplingExpression celExpressionOneEqualsOne2 =
        new CelBasedSamplingExpression(
            CelCompilerFactory.standardCelCompilerBuilder().build().compile("1 == 1").getAst(),
            Sampler.alwaysOn());

    assertThat(celExpressionOneEqualsOne1).isEqualTo(celExpressionOneEqualsOne2);

    CelBasedSamplingExpression celExpressionTwoEqualsTwo =
        new CelBasedSamplingExpression(
            CelCompilerFactory.standardCelCompilerBuilder().build().compile("2 == 2").getAst(),
            Sampler.alwaysOn());

    assertThat(celExpressionOneEqualsOne1).isNotEqualTo(celExpressionTwoEqualsTwo);

    CelBasedSamplingExpression celExpressionOneEqualsOneSamplerOff =
        new CelBasedSamplingExpression(
            CelCompilerFactory.standardCelCompilerBuilder().build().compile("1 == 1").getAst(),
            Sampler.alwaysOff());
    assertThat(celExpressionOneEqualsOne1).isNotEqualTo(celExpressionOneEqualsOneSamplerOff);
  }

  @Test
  void testHashCode() throws CelValidationException {
    CelBasedSamplingExpression celExpression1 =
        new CelBasedSamplingExpression(
            CelCompilerFactory.standardCelCompilerBuilder().build().compile("1 == 1").getAst(),
            Sampler.alwaysOn());
    int expectedHashCode1 = celExpression1.hashCode();
    int expectedHashCode2 = celExpression1.hashCode();

    assertThat(expectedHashCode1).isEqualTo(expectedHashCode2);

    CelBasedSamplingExpression celExpression2 =
        new CelBasedSamplingExpression(
            CelCompilerFactory.standardCelCompilerBuilder().build().compile("1 == 1").getAst(),
            Sampler.alwaysOn());

    assertThat(expectedHashCode1).isEqualTo(celExpression2.hashCode());
  }
}
