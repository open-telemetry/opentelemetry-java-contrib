/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.awsxray;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.sdk.trace.samplers.SamplingDecision;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;
import javax.annotation.Nullable;

final class AwsSamplingResult implements SamplingResult {

  // OTel trace state is a space shared with other vendors with a 256 character limit
  // We keep the key and values as short as possible while still identifiable
  public static final String AWS_XRAY_SAMPLING_RULE_TRACE_STATE_KEY = "xrsr";

  private final SamplingDecision decision;
  private final Attributes attributes;
  @Nullable private final String samplingRuleName;

  private AwsSamplingResult(
      SamplingDecision decision, Attributes attributes, @Nullable String samplingRuleName) {
    this.decision = decision;
    this.attributes = attributes;
    this.samplingRuleName = samplingRuleName;
  }

  static AwsSamplingResult create(
      SamplingDecision decision, Attributes attributes, @Nullable String samplingRuleName) {
    return new AwsSamplingResult(decision, attributes, samplingRuleName);
  }

  @Override
  public SamplingDecision getDecision() {
    return decision;
  }

  @Override
  public Attributes getAttributes() {
    return attributes;
  }

  @Override
  public TraceState getUpdatedTraceState(TraceState parentTraceState) {
    if (parentTraceState.get(AWS_XRAY_SAMPLING_RULE_TRACE_STATE_KEY) == null
        && this.samplingRuleName != null) {
      return parentTraceState.toBuilder()
          .put(AWS_XRAY_SAMPLING_RULE_TRACE_STATE_KEY, samplingRuleName)
          .build();
    }
    return parentTraceState;
  }
}
