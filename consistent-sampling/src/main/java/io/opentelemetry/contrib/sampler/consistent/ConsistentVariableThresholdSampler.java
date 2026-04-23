/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.sampler.consistent;

import static io.opentelemetry.contrib.sampler.consistent.ConsistentSamplingUtil.calculateSamplingProbability;
import static io.opentelemetry.contrib.sampler.consistent.ConsistentSamplingUtil.calculateThreshold;
import static io.opentelemetry.contrib.sampler.consistent.ConsistentSamplingUtil.checkThreshold;
import static io.opentelemetry.contrib.sampler.consistent.ConsistentSamplingUtil.getInvalidThreshold;
import static io.opentelemetry.contrib.sampler.consistent.ConsistentSamplingUtil.getMaxThreshold;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.extension.incubator.trace.samplers.ComposableSampler;
import io.opentelemetry.sdk.extension.incubator.trace.samplers.SamplingIntent;
import io.opentelemetry.sdk.trace.data.LinkData;
import java.util.List;
import java.util.function.Function;

public class ConsistentVariableThresholdSampler implements ComposableSampler {

  private volatile long threshold;
  private volatile String description = "";

  protected ConsistentVariableThresholdSampler(double samplingProbability) {
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
    long threshold = this.threshold;
    if (threshold == getMaxThreshold()) {
      return SamplingIntent.create(
          getInvalidThreshold(), false, Attributes.empty(), Function.identity());
    }
    return SamplingIntent.create(threshold, true, Attributes.empty(), Function.identity());
  }

  @Override
  public String getDescription() {
    return description;
  }

  public long getThreshold() {
    return threshold;
  }

  public void setSamplingProbability(double samplingProbability) {
    updateSamplingProbability(samplingProbability);
  }

  private void updateSamplingProbability(double samplingProbability) {
    long threshold = calculateThreshold(samplingProbability);
    checkThreshold(threshold);
    this.threshold = threshold;

    String thresholdString;
    if (threshold == getMaxThreshold()) {
      thresholdString = "max";
    } else {
      thresholdString =
          ConsistentSamplingUtil.appendLast56BitHexEncodedWithoutTrailingZeros(
                  new StringBuilder(), threshold)
              .toString();
    }

    // tiny eventual consistency where the description would be out of date with the threshold,
    // but this doesn't really matter
    this.description =
        "ConsistentVariableThresholdSampler{threshold="
            + thresholdString
            + ", sampling probability="
            + calculateSamplingProbability(threshold)
            + "}";
  }
}
