package io.opentelemetry.contrib.messaging.wrappers.semconv;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Context;
import io.opentelemetry.contrib.messaging.wrappers.MessagingSpanCustomizer;

import static io.opentelemetry.semconv.ErrorAttributes.ERROR_TYPE;
import static io.opentelemetry.semconv.incubating.MessagingIncubatingAttributes.*;

public class DefaultMessagingProcessSpanCustomizer<REQUEST extends MessagingProcessRequest, RESPONSE extends MessagingProcessResponse<?>>
        implements MessagingSpanCustomizer<REQUEST, RESPONSE> {

  public Context onStart(SpanBuilder spanBuilder, Context parentContext, REQUEST request) {
    if (request == null || spanBuilder == null) {
      return parentContext;
    }
    setAttributeIfNotNull(spanBuilder, MESSAGING_OPERATION_NAME, request.getOperationName());
    setAttributeIfNotNull(spanBuilder, MESSAGING_SYSTEM, request.getSystem());
    setAttributeIfNotNull(spanBuilder, MESSAGING_CONSUMER_GROUP_NAME, request.getConsumerGroupName());
    if (request.isAnonymousDestination()) {
      spanBuilder.setAttribute(MESSAGING_DESTINATION_ANONYMOUS, request.isAnonymousDestination());
    }
    setAttributeIfNotNull(spanBuilder, MESSAGING_DESTINATION_NAME, request.getDestination());
    setAttributeIfNotNull(spanBuilder, MESSAGING_DESTINATION_SUBSCRIPTION_NAME, request.getDestinationSubscriptionName());
    setAttributeIfNotNull(spanBuilder, MESSAGING_DESTINATION_TEMPLATE, request.getDestinationTemplate());
    if (request.isTemporaryDestination()) {
      spanBuilder.setAttribute(MESSAGING_DESTINATION_TEMPORARY, request.isTemporaryDestination());
    }
    setAttributeIfNotNull(spanBuilder, MESSAGING_OPERATION_TYPE, request.getOperationType());
    setAttributeIfNotNull(spanBuilder, MESSAGING_CLIENT_ID, request.getClientId());
    setAttributeIfNotNull(spanBuilder, MESSAGING_DESTINATION_PARTITION_ID, request.getDestinationPartitionId());
    setAttributeIfNotNull(spanBuilder, MESSAGING_MESSAGE_CONVERSATION_ID, request.getConversationId());
    setAttributeIfNotNull(spanBuilder, MESSAGING_MESSAGE_ID, request.getMessageId());
    setAttributeIfNotNull(spanBuilder, MESSAGING_MESSAGE_BODY_SIZE, request.getMessageBodySize());
    setAttributeIfNotNull(spanBuilder, MESSAGING_MESSAGE_ENVELOPE_SIZE, request.getMessageEnvelopeSize());

    return parentContext;
  }

  public void onEnd(Span span, Context context, REQUEST request, RESPONSE response, Throwable t) {
    if (t != null) {
      span.recordException(t);
      span.setAttribute(ERROR_TYPE, t.getClass().getCanonicalName());
      span.setStatus(StatusCode.ERROR, t.getMessage());
    }
  }

  protected <T> void setAttributeIfNotNull(SpanBuilder spanBuilder, AttributeKey<T> attributeKey, T value) {
    if (value == null) {
      return;
    }
    spanBuilder.setAttribute(attributeKey, value);
  }

  protected <T> void setAttributeIfNotNull(Span span, AttributeKey<T> attributeKey, T value) {
    if (value == null) {
      return;
    }
    span.setAttribute(attributeKey, value);
  }

  public static <REQUEST extends MessagingProcessRequest, RESPONSE extends MessagingProcessResponse<?>> MessagingSpanCustomizer<REQUEST, RESPONSE> create() {
    return new DefaultMessagingProcessSpanCustomizer<>();
  }

  DefaultMessagingProcessSpanCustomizer() {}
}
