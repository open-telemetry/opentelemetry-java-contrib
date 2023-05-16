/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.awsxray;

import static java.util.Objects.requireNonNull;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.export.SpanExporter;

public class AwsMetricAttributesSpanExporterBuilder {

  // Defaults
  private static final MetricAttributeGenerator DEFAULT_GENERATOR =
      new AwsMetricAttributeGenerator();

  // Required builder elements
  private final SpanExporter delegate;
  private final Resource resource;

  // Optional builder elements
  private MetricAttributeGenerator generator = DEFAULT_GENERATOR;

  public static AwsMetricAttributesSpanExporterBuilder create(
      SpanExporter delegate, Resource resource) {
    return new AwsMetricAttributesSpanExporterBuilder(delegate, resource);
  }

  private AwsMetricAttributesSpanExporterBuilder(SpanExporter delegate, Resource resource) {
    this.delegate = delegate;
    this.resource = resource;
  }

  /**
   * Sets the generator used to generate attributes used spancs exported by the exporter. If unset,
   * defaults to {@link #DEFAULT_GENERATOR}. Must not be null.
   */
  @CanIgnoreReturnValue
  public AwsMetricAttributesSpanExporterBuilder setGenerator(MetricAttributeGenerator generator) {
    requireNonNull(generator, "generator");
    this.generator = generator;
    return this;
  }

  public AwsMetricAttributesSpanExporter build() {
    return AwsMetricAttributesSpanExporter.create(delegate, generator, resource);
  }
}
