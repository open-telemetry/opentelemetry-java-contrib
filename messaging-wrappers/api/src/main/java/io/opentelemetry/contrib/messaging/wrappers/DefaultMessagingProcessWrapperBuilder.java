package io.opentelemetry.contrib.messaging.wrappers;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.contrib.messaging.wrappers.semconv.DefaultMessagingProcessSpanCustomizer;
import io.opentelemetry.contrib.messaging.wrappers.semconv.MessagingProcessRequest;
import io.opentelemetry.contrib.messaging.wrappers.semconv.MessagingProcessResponse;

import java.util.ArrayList;
import java.util.List;

public class DefaultMessagingProcessWrapperBuilder<REQUEST extends MessagingProcessRequest, RESPONSE extends MessagingProcessResponse<?>> {

  private OpenTelemetry openTelemetry;

  private TextMapGetter<REQUEST> textMapGetter;

  private List<MessagingSpanCustomizer<REQUEST, RESPONSE>> spanCustomizers;

  public static <REQUEST extends MessagingProcessRequest, RESPONSE extends MessagingProcessResponse<?>> DefaultMessagingProcessWrapperBuilder<REQUEST, RESPONSE> create() {
    return new DefaultMessagingProcessWrapperBuilder<>();
  }

  public DefaultMessagingProcessWrapperBuilder<REQUEST, RESPONSE> openTelemetry(OpenTelemetry openTelemetry) {
    this.openTelemetry = openTelemetry;
    return this;
  }

  public DefaultMessagingProcessWrapperBuilder<REQUEST, RESPONSE> textMapGetter(TextMapGetter<REQUEST> textMapGetter) {
    this.textMapGetter = textMapGetter;
    return this;
  }

  public DefaultMessagingProcessWrapperBuilder<REQUEST, RESPONSE> spanCustomizers(
          List<MessagingSpanCustomizer<REQUEST, RESPONSE>> spanCustomizers) {
    this.spanCustomizers = spanCustomizers;
    return this;
  }

  public DefaultMessagingProcessWrapperBuilder<REQUEST, RESPONSE> addSpanCustomizers(
          MessagingSpanCustomizer<REQUEST, RESPONSE> spanCustomizer) {
    this.spanCustomizers.add(spanCustomizer);
    return this;
  }

  public MessagingProcessWrapper<REQUEST, RESPONSE> build() {
    return new MessagingProcessWrapper<>(this.openTelemetry, this.textMapGetter, this.spanCustomizers);
  }

  private DefaultMessagingProcessWrapperBuilder() {
    // init by default
    this.openTelemetry = GlobalOpenTelemetry.get();
    this.spanCustomizers = new ArrayList<>();
    this.spanCustomizers.add(DefaultMessagingProcessSpanCustomizer.create());
  }
}
