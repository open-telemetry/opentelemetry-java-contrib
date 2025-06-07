/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.sampler;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import dev.cel.common.CelAbstractSyntaxTree;
import dev.cel.common.CelValidationException;
import dev.cel.compiler.CelCompilerFactory;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import org.junit.jupiter.api.Test;

class CelBasedSamplingExpressionTest {

  static final String expression = "1 == 1";

  private static CelBasedSamplingExpression createCelBasedSamplingExpression(
      String expression, Sampler sampler) throws CelValidationException {
    CelAbstractSyntaxTree ast =
        CelCompilerFactory.standardCelCompilerBuilder().build().compile(expression).getAst();
    return new CelBasedSamplingExpression(ast, sampler);
  }

  private static CelBasedSamplingExpression createCelBasedSamplingExpression(String expression)
      throws CelValidationException {
    return createCelBasedSamplingExpression(expression, Sampler.alwaysOn());
  }

  private static CelBasedSamplingExpression createCelBasedSamplingExpression()
      throws CelValidationException {
    return createCelBasedSamplingExpression(expression);
  }

  @Test
  public void testThatThrowsOnNullParameter() throws CelValidationException {
    CelAbstractSyntaxTree ast =
        CelCompilerFactory.standardCelCompilerBuilder().build().compile(expression).getAst();
    assertThatExceptionOfType(NullPointerException.class)
        .isThrownBy(() -> new CelBasedSamplingExpression(ast, null));

    assertThatExceptionOfType(NullPointerException.class)
        .isThrownBy(() -> new CelBasedSamplingExpression(null, Sampler.alwaysOn()));
  }

  @Test
  void testToString() throws CelValidationException {
    CelBasedSamplingExpression celExpression = createCelBasedSamplingExpression();
    String expected =
        "CelBasedSamplingExpression{expression='" + expression + "', delegate=AlwaysOnSampler}";
    assertEquals(expected, celExpression.toString());
  }

  @Test
  void testEquals() throws CelValidationException {
    CelBasedSamplingExpression celExpression1 = createCelBasedSamplingExpression();

    assertEquals(celExpression1, celExpression1);
    assertFalse(celExpression1.equals(null));

    CelBasedSamplingExpression celExpression2 = createCelBasedSamplingExpression();

    assertEquals(celExpression1, celExpression2);

    assertNotEquals(celExpression1, createCelBasedSamplingExpression("2 == 2"));
    assertNotEquals(
        celExpression1, createCelBasedSamplingExpression(expression, Sampler.alwaysOff()));
  }

  @Test
  void testHashCode() throws CelValidationException {
    CelBasedSamplingExpression celExpression1 = createCelBasedSamplingExpression();
    int expectedHashCode1 = celExpression1.hashCode();
    int expectedHashCode2 = celExpression1.hashCode();

    assertEquals(expectedHashCode1, expectedHashCode2);

    CelBasedSamplingExpression celExpression2 = createCelBasedSamplingExpression();

    assertEquals(expectedHashCode1, celExpression2.hashCode());
  }
}
