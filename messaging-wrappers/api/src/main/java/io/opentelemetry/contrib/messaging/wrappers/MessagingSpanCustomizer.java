package io.opentelemetry.contrib.messaging.wrappers;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.context.Context;
import io.opentelemetry.contrib.messaging.wrappers.semconv.MessagingProcessRequest;
import io.opentelemetry.contrib.messaging.wrappers.semconv.MessagingProcessResponse;

public interface MessagingSpanCustomizer<REQUEST extends MessagingProcessRequest, RESPONSE extends MessagingProcessResponse<?>> {

  Context onStart(SpanBuilder spanBuilder, Context parentContext, REQUEST request);

  void onEnd(Span span, Context context, REQUEST request, RESPONSE response, Throwable t);
}
