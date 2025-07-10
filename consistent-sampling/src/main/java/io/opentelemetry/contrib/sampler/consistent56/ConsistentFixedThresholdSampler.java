/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.sampler.consistent56;

import static io.opentelemetry.contrib.sampler.consistent56.ConsistentSamplingUtil.calculateSamplingProbability;
import static io.opentelemetry.contrib.sampler.consistent56.ConsistentSamplingUtil.calculateThreshold;
import static io.opentelemetry.contrib.sampler.consistent56.ConsistentSamplingUtil.checkThreshold;
import static io.opentelemetry.contrib.sampler.consistent56.ConsistentSamplingUtil.getInvalidThreshold;
import static io.opentelemetry.contrib.sampler.consistent56.ConsistentSamplingUtil.getMaxThreshold;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.data.LinkData;
import java.util.List;

public class ConsistentFixedThresholdSampler extends ConsistentSampler {

  private final long threshold;
  private final String description;

  protected ConsistentFixedThresholdSampler(long threshold) {
    this.threshold = getThreshold(threshold);
    this.description = getThresholdDescription(threshold);
  }

  protected ConsistentFixedThresholdSampler(double samplingProbability) {
    this(calculateThreshold(samplingProbability));
  }

  private static long getThreshold(long threshold) {
    checkThreshold(threshold);
    return threshold;
  }

  private static String getThresholdDescription(long threshold) {
    String thresholdString;
    if (threshold == getMaxThreshold()) {
      thresholdString = "max";
    } else {
      thresholdString =
          ConsistentSamplingUtil.appendLast56BitHexEncodedWithoutTrailingZeros(
                  new StringBuilder(), threshold)
              .toString();
    }

    return "ConsistentFixedThresholdSampler{threshold="
        + thresholdString
        + ", sampling probability="
        + calculateSamplingProbability(threshold)
        + "}";
  }

  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public SamplingIntent getSamplingIntent(
      Context parentContext,
      String name,
      SpanKind spanKind,
      Attributes attributes,
      List<LinkData> parentLinks) {

    return () -> {
      if (threshold == getMaxThreshold()) {
        return getInvalidThreshold();
      }
      return threshold;
    };
  }
}
