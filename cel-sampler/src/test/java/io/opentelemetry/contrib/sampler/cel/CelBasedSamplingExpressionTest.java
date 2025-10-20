/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.sampler.cel;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

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
    assertEquals(expected, celExpression.toString());
  }

  @Test
  void testEquals() throws CelValidationException {
    CelBasedSamplingExpression celExpressionOneEqualsOne1 =
        new CelBasedSamplingExpression(
            CelCompilerFactory.standardCelCompilerBuilder().build().compile("1 == 1").getAst(),
            Sampler.alwaysOn());

    assertEquals(celExpressionOneEqualsOne1, celExpressionOneEqualsOne1);
    assertNotEquals(celExpressionOneEqualsOne1, null);

    CelBasedSamplingExpression celExpressionOneEqualsOne2 =
        new CelBasedSamplingExpression(
            CelCompilerFactory.standardCelCompilerBuilder().build().compile("1 == 1").getAst(),
            Sampler.alwaysOn());

    assertEquals(celExpressionOneEqualsOne1, celExpressionOneEqualsOne2);

    CelBasedSamplingExpression celExpressionTwoEqualsTwo =
        new CelBasedSamplingExpression(
            CelCompilerFactory.standardCelCompilerBuilder().build().compile("2 == 2").getAst(),
            Sampler.alwaysOn());

    assertNotEquals(celExpressionOneEqualsOne1, celExpressionTwoEqualsTwo);

    CelBasedSamplingExpression celExpressionOneEqualsOneSamplerOff =
        new CelBasedSamplingExpression(
            CelCompilerFactory.standardCelCompilerBuilder().build().compile("1 == 1").getAst(),
            Sampler.alwaysOff());
    assertNotEquals(celExpressionOneEqualsOne1, celExpressionOneEqualsOneSamplerOff);
  }

  @Test
  void testHashCode() throws CelValidationException {
    CelBasedSamplingExpression celExpression1 =
        new CelBasedSamplingExpression(
            CelCompilerFactory.standardCelCompilerBuilder().build().compile("1 == 1").getAst(),
            Sampler.alwaysOn());
    int expectedHashCode1 = celExpression1.hashCode();
    int expectedHashCode2 = celExpression1.hashCode();

    assertEquals(expectedHashCode1, expectedHashCode2);

    CelBasedSamplingExpression celExpression2 =
        new CelBasedSamplingExpression(
            CelCompilerFactory.standardCelCompilerBuilder().build().compile("1 == 1").getAst(),
            Sampler.alwaysOn());

    assertEquals(expectedHashCode1, celExpression2.hashCode());
  }
}
