package io.opentelemetry.contrib.messaging.wrappers;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.contrib.messaging.wrappers.semconv.DefaultMessagingAttributesGetter;
import io.opentelemetry.contrib.messaging.wrappers.semconv.MessagingProcessRequest;
import io.opentelemetry.instrumentation.api.incubator.semconv.messaging.MessageOperation;
import io.opentelemetry.instrumentation.api.incubator.semconv.messaging.MessagingAttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class DefaultMessagingProcessWrapperBuilder<REQUEST extends MessagingProcessRequest> {

  private OpenTelemetry openTelemetry;

  protected TextMapGetter<REQUEST> textMapGetter;

  protected List<AttributesExtractor<REQUEST, Void>> attributesExtractors;

  public DefaultMessagingProcessWrapperBuilder<REQUEST> openTelemetry(OpenTelemetry openTelemetry) {
    this.openTelemetry = openTelemetry;
    return this;
  }

  public DefaultMessagingProcessWrapperBuilder<REQUEST> textMapGetter(TextMapGetter<REQUEST> textMapGetter) {
    this.textMapGetter = textMapGetter;
    return this;
  }

  /**
   * This method overrides the original items.
   * <p>See {@link DefaultMessagingProcessWrapperBuilder#addAttributesExtractor} if you just want to append one.</p>
   * */
  public DefaultMessagingProcessWrapperBuilder<REQUEST> attributesExtractors(
      Collection<AttributesExtractor<REQUEST, Void>> attributesExtractors) {
    this.attributesExtractors = new ArrayList<>();
    this.attributesExtractors.addAll(attributesExtractors);
    return this;
  }

  public DefaultMessagingProcessWrapperBuilder<REQUEST> addAttributesExtractor(
      AttributesExtractor<REQUEST, Void> attributesExtractor) {
    this.attributesExtractors.add(attributesExtractor);
    return this;
  }

  public MessagingProcessWrapper<REQUEST> build() {
    return new MessagingProcessWrapper<>(this.openTelemetry == null ? GlobalOpenTelemetry.get() : this.openTelemetry,
            this.textMapGetter, this.attributesExtractors);
  }

  protected DefaultMessagingProcessWrapperBuilder() {
    // init attributes extractors by default
    this.attributesExtractors = new ArrayList<>();
    this.attributesExtractors.add(MessagingAttributesExtractor.create(
            DefaultMessagingAttributesGetter.create(), MessageOperation.PROCESS));
  }
}
