/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.awsxray;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.data.DelegatingSpanData;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import javax.annotation.concurrent.Immutable;

/**
 * This exporter will update a span with metric attributes before exporting. It depends on a {@link
 * SpanExporter} being provided on instantiation, which the AwsSpanMetricsExporter will delegate
 * export to. Also, a {@link MetricAttributeGenerator} must be provided, which will provide a means
 * to determine attributes which should be applied to the span. Finally, a {@link Resource} must be
 * provided, which is used to generate metric attributes.
 *
 * <p>This exporter should be coupled with the {@link AwsSpanMetricsProcessor} using the same {@link
 * MetricAttributeGenerator}. This will result in metrics and spans being produced with common
 * attributes.
 */
@Immutable
public class AwsMetricAttributesSpanExporter implements SpanExporter {

  private final SpanExporter delegate;
  private final MetricAttributeGenerator generator;
  private final Resource resource;

  /** Use {@link AwsMetricAttributesSpanExporterBuilder} to construct this exporter. */
  static AwsMetricAttributesSpanExporter create(
      SpanExporter delegate, MetricAttributeGenerator generator, Resource resource) {
    return new AwsMetricAttributesSpanExporter(delegate, generator, resource);
  }

  private AwsMetricAttributesSpanExporter(
      SpanExporter delegate, MetricAttributeGenerator generator, Resource resource) {
    this.delegate = delegate;
    this.generator = generator;
    this.resource = resource;
  }

  @Override
  public CompletableResultCode export(Collection<SpanData> spans) {
    List<SpanData> modifiedSpans = addMetricAttributes(spans);
    return delegate.export(modifiedSpans);
  }

  @Override
  public CompletableResultCode flush() {
    return delegate.flush();
  }

  @Override
  public CompletableResultCode shutdown() {
    return delegate.shutdown();
  }

  @Override
  public void close() {
    delegate.close();
  }

  private List<SpanData> addMetricAttributes(Collection<SpanData> spans) {
    List<SpanData> modifiedSpans = new ArrayList<>();

    for (SpanData span : spans) {
      Attributes attributes = generator.generateMetricAttributesFromSpan(span, resource);
      if (!attributes.isEmpty()) {
        span = wrapSpanWithAttributes(span, attributes);
      }
      modifiedSpans.add(span);
    }

    return modifiedSpans;
  }

  /**
   * {@link #export} works with a {@link SpanData}, which does not permit modification. However, we
   * need to add derived metric attributes to the span. To work around this, we will wrap the
   * SpanData with a {@link DelegatingSpanData} that simply passes through all API calls, except for
   * those pertaining to Attributes, i.e. {@link SpanData#getAttributes()} and {@link
   * SpanData#getTotalAttributeCount} APIs.
   *
   * <p>See https://github.com/open-telemetry/opentelemetry-specification/issues/1089 for more
   * context on this approach.
   */
  private static SpanData wrapSpanWithAttributes(SpanData span, Attributes attributes) {
    Attributes originalAttributes = span.getAttributes();
    Attributes replacementAttributes = originalAttributes.toBuilder().putAll(attributes).build();

    int newAttributeKeyCount = 0;
    for (Entry<AttributeKey<?>, Object> entry : attributes.asMap().entrySet()) {
      if (originalAttributes.get(entry.getKey()) == null) {
        newAttributeKeyCount++;
      }
    }
    int originalTotalAttributeCount = span.getTotalAttributeCount();
    int replacementTotalAttributeCount = originalTotalAttributeCount + newAttributeKeyCount;

    return new DelegatingSpanData(span) {
      @Override
      public Attributes getAttributes() {
        return replacementAttributes;
      }

      @Override
      public int getTotalAttributeCount() {
        return replacementTotalAttributeCount;
      }
    };
  }
}
