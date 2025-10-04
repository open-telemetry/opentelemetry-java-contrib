/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.messaging.wrappers;

import static java.util.Objects.requireNonNull;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.SpanNameExtractor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;

public class MessagingProcessWrapperBuilder<REQUEST> {

  @Nullable private OpenTelemetry openTelemetry;

  @Nullable protected TextMapGetter<REQUEST> textMapGetter;

  @Nullable protected SpanNameExtractor<REQUEST> spanNameExtractor;

  @Nullable protected List<AttributesExtractor<REQUEST, Void>> attributesExtractors;

  @CanIgnoreReturnValue
  public MessagingProcessWrapperBuilder<REQUEST> openTelemetry(OpenTelemetry openTelemetry) {
    this.openTelemetry = openTelemetry;
    return this;
  }

  @CanIgnoreReturnValue
  public MessagingProcessWrapperBuilder<REQUEST> textMapGetter(
      TextMapGetter<REQUEST> textMapGetter) {
    this.textMapGetter = textMapGetter;
    return this;
  }

  @CanIgnoreReturnValue
  public MessagingProcessWrapperBuilder<REQUEST> spanNameExtractor(
      SpanNameExtractor<REQUEST> spanNameExtractor) {
    this.spanNameExtractor = spanNameExtractor;
    return this;
  }

  @CanIgnoreReturnValue
  public MessagingProcessWrapperBuilder<REQUEST> attributesExtractors(
      Collection<AttributesExtractor<REQUEST, Void>> attributesExtractors) {
    this.attributesExtractors = new ArrayList<>();
    this.attributesExtractors.addAll(attributesExtractors);
    return this;
  }

  public MessagingProcessWrapper<REQUEST> build() {
    requireNonNull(this.spanNameExtractor);
    requireNonNull(this.attributesExtractors);
    return new MessagingProcessWrapper<>(
        this.openTelemetry == null ? GlobalOpenTelemetry.get() : this.openTelemetry,
        this.textMapGetter == null ? NoopTextMapGetter.create() : this.textMapGetter,
        this.spanNameExtractor,
        this.attributesExtractors);
  }

  protected MessagingProcessWrapperBuilder() {}
}
