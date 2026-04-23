/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.sampler.consistent;

import static io.opentelemetry.contrib.sampler.consistent.ConsistentSamplingUtil.INVALID_THRESHOLD;
import static io.opentelemetry.contrib.sampler.consistent.ConsistentSamplingUtil.isValidThreshold;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.extension.incubator.trace.samplers.ComposableSampler;
import io.opentelemetry.sdk.extension.incubator.trace.samplers.SamplingIntent;
import io.opentelemetry.sdk.trace.data.LinkData;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

/**
 * A consistent sampler that queries all its delegate samplers for their sampling threshold, and
 * uses the minimum threshold value received.
 */
@Immutable
final class ConsistentAnyOf implements ComposableSampler {

  private final ComposableSampler[] delegates;
  private final String description;

  ConsistentAnyOf(@Nullable ComposableSampler... delegates) {
    if (delegates == null || delegates.length == 0) {
      throw new IllegalArgumentException(
          "At least one delegate must be specified for ConsistentAnyOf");
    }

    this.delegates = delegates;

    this.description =
        Stream.of(delegates)
            .map(ComposableSampler::getDescription)
            .collect(Collectors.joining(",", "ConsistentAnyOf{", "}"));
  }

  @Override
  public SamplingIntent getSamplingIntent(
      Context parentContext,
      String traceId,
      String name,
      SpanKind spanKind,
      Attributes attributes,
      List<LinkData> parentLinks) {

    SamplingIntent[] intents = new SamplingIntent[delegates.length];

    // If any of the delegates provides a valid threshold, the resulting threshold is the minimum
    // value T from the set of those valid threshold values, otherwise it is invalid threshold.
    long minimumThreshold = INVALID_THRESHOLD;

    // If any of the delegates returning the threshold value equal to T returns true upon calling
    // its isThresholdReliable() method, the resulting thresholdReliable is true,
    // otherwise it is false.
    boolean thresholdReliable = false;

    int k = 0;
    for (ComposableSampler delegate : delegates) {
      SamplingIntent delegateIntent =
          delegate.getSamplingIntent(
              parentContext, traceId, name, spanKind, attributes, parentLinks);
      long delegateThreshold = delegateIntent.getThreshold();
      if (isValidThreshold(delegateThreshold)) {
        if (isValidThreshold(minimumThreshold)) {
          if (delegateThreshold == minimumThreshold) {
            if (delegateIntent.isThresholdReliable()) {
              thresholdReliable = true;
            }
          } else if (delegateThreshold < minimumThreshold) {
            minimumThreshold = delegateThreshold;
            thresholdReliable = delegateIntent.isThresholdReliable();
          }
        } else {
          minimumThreshold = delegateThreshold;
          thresholdReliable = delegateIntent.isThresholdReliable();
        }
      }
      intents[k++] = delegateIntent;
    }

    AttributesBuilder builder = Attributes.builder();
    for (SamplingIntent intent : intents) {
      builder = builder.putAll(intent.getAttributes());
    }
    Attributes mergedAttributes = builder.build();

    Function<TraceState, TraceState> composed =
        s -> {
          TraceState state = s;
          for (SamplingIntent intent : intents) {
            state = intent.getTraceStateUpdater().apply(state);
          }
          return state;
        };

    return SamplingIntent.create(minimumThreshold, thresholdReliable, mergedAttributes, composed);
  }

  @Override
  public String getDescription() {
    return description;
  }
}
