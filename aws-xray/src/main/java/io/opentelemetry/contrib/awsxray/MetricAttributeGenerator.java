/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.awsxray;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.data.SpanData;

/**
 * Metric attribute generator defines an interface for classes that can generate specific attributes
 * to be used by an {@link AwsSpanMetricsProcessor} to produce metrics and by {@link
 * AwsMetricAttributesSpanExporter} to wrap the original span.
 */
public interface MetricAttributeGenerator {

  /**
   * Given a span and associated resource, produce meaningful metric attributes for metrics produced
   * from the span. If no metrics should be generated from this span, return {@link
   * Attributes#empty()}.
   *
   * @param span - SpanData to be used to generate metric attributes.
   * @param resource - Resource associated with Span to be used to generate metric attributes.
   * @return A set of zero or more attributes. Must not return null.
   */
  Attributes generateMetricAttributesFromSpan(SpanData span, Resource resource);
}
