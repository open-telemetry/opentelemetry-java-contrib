/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.sampler.consistent;

import static io.opentelemetry.contrib.sampler.consistent.ConsistentSamplingUtil.MAX_THRESHOLD;
import static io.opentelemetry.contrib.sampler.consistent.ConsistentSamplingUtil.calculateSamplingProbability;
import static io.opentelemetry.contrib.sampler.consistent.ConsistentSamplingUtil.calculateThreshold;
import static io.opentelemetry.contrib.sampler.consistent.ConsistentSamplingUtil.encodeLast56BitHexWithoutTrailingZeros;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.extension.incubator.trace.samplers.ComposableSampler;
import io.opentelemetry.sdk.extension.incubator.trace.samplers.SamplingIntent;
import io.opentelemetry.sdk.trace.data.LinkData;
import java.util.List;
import java.util.function.Function;

/**
 * A consistent sampler applying a fixed sampling probability that can be updated at runtime via
 * {@link #setSamplingProbability(double)}.
 */
public final class ConsistentVariableThresholdSampler implements ComposableSampler {

  private volatile long threshold;
  private volatile String description = "";

  ConsistentVariableThresholdSampler(double samplingProbability) {
    updateSamplingProbability(samplingProbability);
  }

  @Override
  public SamplingIntent getSamplingIntent(
      Context parentContext,
      String traceId,
      String name,
      SpanKind spanKind,
      Attributes attributes,
      List<LinkData> parentLinks) {
    return SamplingIntent.create(threshold, true, Attributes.empty(), Function.identity());
  }

  @Override
  public String getDescription() {
    return description;
  }

  /** Returns the current rejection threshold. Visible for testing. */
  long getThreshold() {
    return threshold;
  }

  /** Updates the sampling probability applied by this sampler. */
  public void setSamplingProbability(double samplingProbability) {
    updateSamplingProbability(samplingProbability);
  }

  private void updateSamplingProbability(double samplingProbability) {
    long t = calculateThreshold(samplingProbability);
    this.threshold = t;

    String thresholdString;
    if (t == MAX_THRESHOLD) {
      thresholdString = "max";
    } else {
      thresholdString = encodeLast56BitHexWithoutTrailingZeros(t);
    }

    // tiny eventual consistency where the description could be out of date with the threshold,
    // but this doesn't really matter
    this.description =
        "ConsistentVariableThresholdSampler{threshold="
            + thresholdString
            + ", sampling probability="
            + calculateSamplingProbability(t)
            + "}";
  }
}
