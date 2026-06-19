/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.sampler.consistent;

import static io.opentelemetry.contrib.sampler.consistent.ConsistentSamplingUtil.getInvalidThreshold;
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
      String traceId,
      String name,
      SpanKind spanKind,
      Attributes attributes,
      List<LinkData> parentLinks) {

    SamplingIntent[] intents = new SamplingIntent[delegates.length];

    // If any of the delegates provides a valid threshold, the resulting threshold is the minimum
    // value T from the set of those valid threshold values, otherwise it is invalid threshold.
    long minimumThreshold = getInvalidThreshold();

    // If any of the delegates returning the threshold value equal to T returns true upon calling
    // its isThresholdReliable() method, the resulting isThresholdReliable is true,
    // otherwise it is false.
    boolean isThresholdReliable = false;

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
              isThresholdReliable = true;
            }
          } else if (delegateThreshold < minimumThreshold) {
            minimumThreshold = delegateThreshold;
            isThresholdReliable = delegateIntent.isThresholdReliable();
          }
        } else {
          minimumThreshold = delegateThreshold;
          isThresholdReliable = delegateIntent.isThresholdReliable();
        }
      }
      intents[k++] = delegateIntent;
    }

    long resultingThreshold = minimumThreshold;
    boolean resultingThresholdReliable = isThresholdReliable;

    AttributesBuilder builder = Attributes.builder();
    for (SamplingIntent intent : intents) {
      builder = builder.putAll(intent.getAttributes());
    }
    Attributes mergedAttributes = builder.build();

    Function<TraceState, TraceState> composedUpdater =
        previousState -> {
          TraceState state = previousState;
          for (SamplingIntent intent : intents) {
            state = intent.getTraceStateUpdater().apply(state);
          }
          return state;
        };

    return SamplingIntent.create(
        resultingThreshold, resultingThresholdReliable, mergedAttributes, composedUpdater);
  }

  @Override
  public String getDescription() {
    return description;
  }
}
