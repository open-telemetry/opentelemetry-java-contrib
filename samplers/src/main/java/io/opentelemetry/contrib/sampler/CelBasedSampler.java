/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.sampler;

import static java.util.Objects.requireNonNull;

import dev.cel.common.types.SimpleType;
import dev.cel.compiler.CelCompiler;
import dev.cel.compiler.CelCompilerFactory;
import dev.cel.runtime.CelEvaluationException;
import dev.cel.runtime.CelRuntime;
import dev.cel.runtime.CelRuntimeFactory;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * This sampler accepts a list of {@link CelBasedSamplingExpression}s and tries to match every
 * proposed span against those rules. Every rule describes a span's attribute, a pattern against
 * which to match attribute's value, and a sampler that will make a decision about given span if
 * match was successful.
 *
 * <p>Matching is performed by {@link Pattern}.
 *
 * <p>Provided span kind is checked first and if differs from the one given to {@link
 * #builder(Sampler)}, the default fallback sampler will make a decision.
 *
 * <p>Note that only attributes that were set on {@link SpanBuilder} will be taken into account,
 * attributes set after the span has been started are not used
 *
 * <p>If none of the rules matched, the default fallback sampler will make a decision.
 */
public final class CelBasedSampler implements Sampler {

  private static final Logger logger = Logger.getLogger(CelBasedSampler.class.getName());

  private static final CelCompiler celCompiler =
      CelCompilerFactory.standardCelCompilerBuilder()
          .addVar("name", SimpleType.STRING) // Span name
          .addVar("spanKind", SimpleType.STRING) // Span kind as string
          .addVar("attribute", SimpleType.DYN) // Attributes as a map
          .setResultType(SimpleType.BOOL) // Expression must return boolean
          .build();
  final CelRuntime celRuntime;

  private final Sampler fallback;
  private final List<CelBasedSamplingExpression> expressions;

  public CelBasedSampler(Sampler fallback, List<CelBasedSamplingExpression> expressions) {
    this.fallback = fallback;
    this.expressions = expressions;
    this.celRuntime = CelRuntimeFactory.standardCelRuntimeBuilder().build();
  }

  public static CelBasedSamplerBuilder builder(Sampler fallback) {
    return new CelBasedSamplerBuilder(
        requireNonNull(fallback, "fallback sampler must not be null"), celCompiler);
  }

  @Override
  public SamplingResult shouldSample(
      Context parentContext,
      String traceId,
      String name,
      SpanKind spanKind,
      Attributes attributes,
      List<LinkData> parentLinks) {

    // Prepare the evaluation context with span data
    Map<String, Object> evaluationContext = new HashMap<>();
    evaluationContext.put("name", name);
    evaluationContext.put("spanKind", spanKind.name());
    evaluationContext.put("attribute", convertAttributesToMap(attributes));

    for (CelBasedSamplingExpression expression : expressions) {
      try {
        CelRuntime.Program program = celRuntime.createProgram(expression.abstractSyntaxTree);
        Object result = program.eval(evaluationContext);
        // Happy path: Perform sampling based on the boolean result
        if (result instanceof Boolean && ((Boolean) result)) {
          return expression.delegate.shouldSample(
              parentContext, traceId, name, spanKind, attributes, parentLinks);
        }
        // If result is not boolean, treat as false
        logger.log(
            Level.FINE,
            "Expression '" + expression.expression + "' returned non-boolean result: " + result);
      } catch (CelEvaluationException e) {
        logger.log(
            Level.FINE,
            "Expression '" + expression.expression + "' evaluation error: " + e.getMessage());
      }
    }

    return fallback.shouldSample(parentContext, traceId, name, spanKind, attributes, parentLinks);
  }

  /** Convert OpenTelemetry Attributes to a Map that CEL can work with */
  private static Map<String, Object> convertAttributesToMap(Attributes attributes) {
    Map<String, Object> map = new HashMap<>();
    attributes.forEach(
        (key, value) -> {
          // Convert attribute values to appropriate types
          if (value instanceof String) {
            map.put(key.getKey(), value);
          } else if (value instanceof Long) {
            map.put(key.getKey(), value);
          } else if (value instanceof Double) {
            map.put(key.getKey(), value);
          } else if (value instanceof Boolean) {
            map.put(key.getKey(), value);
          } else {
            // For other types, convert to string
            map.put(key.getKey(), value.toString());
          }
        });
    return map;
  }

  @Override
  public String getDescription() {
    return "CelBasedSampler{" + "fallback=" + fallback + ", expressions=" + expressions + '}';
  }

  @Override
  public String toString() {
    return getDescription();
  }
}
