/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.awsxray;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingDecision;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;
import java.util.List;
import javax.annotation.concurrent.Immutable;

/**
 * This sampler will return the sampling result of the provided {@link #rootSampler}, unless the
 * sampling result contains the sampling decision {@link SamplingDecision#DROP}, in which case, a
 * new sampling result will be returned that is functionally equivalent to the original, except that
 * it contains the sampling decision {@link SamplingDecision#RECORD_ONLY}. This ensures that all
 * spans are recorded, with no change to sampling.
 *
 * <p>The intended use case of this sampler is to provide a means of sending all spans to a
 * processor without having an impact on the sampling rate. This may be desirable if a user wishes
 * to count or otherwise measure all spans produced in a service, without incurring the cost of 100%
 * sampling.
 */
@Immutable
public final class AlwaysRecordSampler implements Sampler {

  private final Sampler rootSampler;

  public static AlwaysRecordSampler create(Sampler rootSampler) {
    return new AlwaysRecordSampler(rootSampler);
  }

  private AlwaysRecordSampler(Sampler rootSampler) {
    this.rootSampler = rootSampler;
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
        rootSampler.shouldSample(parentContext, traceId, name, spanKind, attributes, parentLinks);
    if (result.getDecision() == SamplingDecision.DROP) {
      result = wrapResultWithRecordOnlyResult(result);
    }

    return result;
  }

  @Override
  public String getDescription() {
    return "AlwaysRecordSampler{" + rootSampler.getDescription() + "}";
  }

  private static SamplingResult wrapResultWithRecordOnlyResult(SamplingResult result) {
    return new SamplingResult() {
      @Override
      public SamplingDecision getDecision() {
        return SamplingDecision.RECORD_ONLY;
      }

      @Override
      public Attributes getAttributes() {
        return result.getAttributes();
      }

      @Override
      public TraceState getUpdatedTraceState(TraceState parentTraceState) {
        return result.getUpdatedTraceState(parentTraceState);
      }
    };
  }
}
