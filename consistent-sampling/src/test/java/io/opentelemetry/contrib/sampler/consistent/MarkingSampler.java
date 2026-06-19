/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.sampler.consistent;

import static java.util.Objects.requireNonNull;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.extension.incubator.trace.samplers.ComposableSampler;
import io.opentelemetry.sdk.extension.incubator.trace.samplers.SamplingIntent;
import io.opentelemetry.sdk.trace.data.LinkData;
import java.util.List;
import javax.annotation.concurrent.Immutable;

/**
 * A Composable that creates the same sampling intent as the delegate, but it additionally sets a
 * Span attribute according to the provided attribute key and value. This is used by unit tests, but
 * could be also offered as a general utility.
 */
@Immutable
final class MarkingSampler implements ComposableSampler {

  private final ComposableSampler delegate;
  private final AttributeKey<String> attributeKey;
  private final String attributeValue;

  private final String description;

  /**
   * Constructs a new MarkingSampler
   *
   * @param delegate the delegate sampler
   * @param attributeKey Span attribute key
   * @param attributeValue Span attribute value
   */
  MarkingSampler(
      ComposableSampler delegate, AttributeKey<String> attributeKey, String attributeValue) {
    this.delegate = requireNonNull(delegate);
    this.attributeKey = requireNonNull(attributeKey);
    this.attributeValue = requireNonNull(attributeValue);
    this.description =
        "MarkingSampler{delegate="
            + delegate.getDescription()
            + ",key="
            + attributeKey
            + ",value="
            + attributeValue
            + '}';
  }

  @Override
  public SamplingIntent getSamplingIntent(
      Context parentContext,
      String traceId,
      String name,
      SpanKind spanKind,
      Attributes attributes,
      List<LinkData> parentLinks) {

    SamplingIntent delegateIntent =
        delegate.getSamplingIntent(parentContext, traceId, name, spanKind, attributes, parentLinks);

    AttributesBuilder builder = delegateIntent.getAttributes().toBuilder();
    builder = builder.put(attributeKey, attributeValue);

    return SamplingIntent.create(
        delegateIntent.getThreshold(),
        delegateIntent.isThresholdReliable(),
        builder.build(),
        delegateIntent.getTraceStateUpdater());
  }

  @Override
  public String getDescription() {
    return description;
  }
}
