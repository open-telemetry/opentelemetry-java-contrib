/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.sampler.consistent56;

import static io.opentelemetry.contrib.sampler.consistent56.ConsistentSamplingUtil.getInvalidThreshold;
import static io.opentelemetry.contrib.sampler.consistent56.ConsistentSamplingUtil.isValidThreshold;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.data.LinkData;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

/**
 * A consistent sampler that queries all its delegate samplers for their sampling threshold, and
 * uses the minimum threshold value received.
 */
@Immutable
final class ConsistentAnyOf extends ConsistentSampler {

  private final ComposableSampler[] delegates;

  private final String description;

  /**
   * Constructs a new consistent AnyOf sampler using the provided delegate samplers.
   *
   * @param delegates the delegate samplers
   */
  ConsistentAnyOf(@Nullable ComposableSampler... delegates) {
    if (delegates == null || delegates.length == 0) {
      throw new IllegalArgumentException(
          "At least one delegate must be specified for ConsistentAnyOf");
    }

    this.delegates = delegates;

    this.description =
        Stream.of(delegates)
            .map(Object::toString)
            .collect(Collectors.joining(",", "ConsistentAnyOf{", "}"));
  }

  @Override
  public SamplingIntent getSamplingIntent(
      Context parentContext,
      String name,
      SpanKind spanKind,
      Attributes attributes,
      List<LinkData> parentLinks) {

    SamplingIntent[] intents = new SamplingIntent[delegates.length];
    int k = 0;
    long minimumThreshold = getInvalidThreshold();
    for (ComposableSampler delegate : delegates) {
      SamplingIntent delegateIntent =
          delegate.getSamplingIntent(parentContext, name, spanKind, attributes, parentLinks);
      long delegateThreshold = delegateIntent.getThreshold();
      if (isValidThreshold(delegateThreshold)) {
        if (isValidThreshold(minimumThreshold)) {
          minimumThreshold = Math.min(delegateThreshold, minimumThreshold);
        } else {
          minimumThreshold = delegateThreshold;
        }
      }
      intents[k++] = delegateIntent;
    }

    long resultingThreshold = minimumThreshold;

    return new SamplingIntent() {
      @Override
      public long getThreshold() {
        return resultingThreshold;
      }

      @Override
      public Attributes getAttributes() {
        AttributesBuilder builder = Attributes.builder();
        for (SamplingIntent intent : intents) {
          builder = builder.putAll(intent.getAttributes());
        }
        return builder.build();
      }

      @Override
      public TraceState updateTraceState(TraceState previousState) {
        for (SamplingIntent intent : intents) {
          previousState = intent.updateTraceState(previousState);
        }
        return previousState;
      }
    };
  }

  @Override
  public String getDescription() {
    return description;
  }
}
