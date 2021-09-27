/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.awsxray;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingDecision;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;
import java.util.List;

class OrElseSampler implements Sampler {

  private final Sampler first;
  private final Sampler second;

  OrElseSampler(Sampler first, Sampler second) {
    this.first = first;
    this.second = second;
  }

  @Override
  public SamplingResult shouldSample(
      Context parentContext,
      String traceId,
      String name,
      SpanKind spanKind,
      Attributes attributes,
      List<LinkData> parentLinks) {
    SamplingResult result =
        first.shouldSample(parentContext, traceId, name, spanKind, attributes, parentLinks);
    if (result.getDecision() != SamplingDecision.DROP) {
      return result;
    }
    return second.shouldSample(parentContext, traceId, name, spanKind, attributes, parentLinks);
  }

  @Override
  public String getDescription() {
    return "OrElse{"
        + "first:"
        + first.getDescription()
        + ", second:"
        + second.getDescription()
        + "}";
  }
}
