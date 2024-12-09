/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.sampler.consistent56;

import static io.opentelemetry.contrib.sampler.consistent56.ConsistentSamplingUtil.getInvalidThreshold;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.data.LinkData;
import java.util.List;
import javax.annotation.concurrent.Immutable;

@Immutable
final class ConsistentAlwaysOffSampler extends ConsistentSampler {

  private static final ConsistentAlwaysOffSampler INSTANCE = new ConsistentAlwaysOffSampler();

  private ConsistentAlwaysOffSampler() {}

  static ConsistentAlwaysOffSampler getInstance() {
    return INSTANCE;
  }

  @Override
  public SamplingIntent getSamplingIntent(
      Context parentContext,
      String name,
      SpanKind spanKind,
      Attributes attributes,
      List<LinkData> parentLinks) {

    return () -> getInvalidThreshold();
  }

  @Override
  public String getDescription() {
    return "ConsistentAlwaysOffSampler";
  }
}
