/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.opentelemetry.contrib.awsxray;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.common.Clock;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingDecision;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;
import java.util.List;

final class RateLimitingSampler implements Sampler {

  private final RateLimiter limiter;
  private final int numPerSecond;

  RateLimitingSampler(int numPerSecond, Clock clock) {
    limiter = new RateLimiter(numPerSecond, numPerSecond, clock);
    this.numPerSecond = numPerSecond;
  }

  @Override
  public SamplingResult shouldSample(
      Context parentContext,
      String traceId,
      String name,
      SpanKind spanKind,
      Attributes attributes,
      List<LinkData> parentLinks) {
    if (limiter.trySpend(1)) {
      return SamplingResult.create(SamplingDecision.RECORD_AND_SAMPLE);
    }
    return SamplingResult.create(SamplingDecision.DROP);
  }

  @Override
  public String getDescription() {
    return "RateLimitingSampler{" + numPerSecond + "}";
  }
}
