package io.opentelemetry.contrib.messaging.wrappers.semconv;

import io.opentelemetry.instrumentation.api.incubator.semconv.messaging.MessagingAttributesGetter;

import javax.annotation.Nullable;
import java.util.List;

public class DefaultMessagingAttributesGetter<REQUEST extends MessagingProcessRequest>
        implements MessagingAttributesGetter<REQUEST, Void> {

    public static <REQUEST extends MessagingProcessRequest> MessagingAttributesGetter<REQUEST, Void> create() {
        return new DefaultMessagingAttributesGetter<>();
    }

    @Nullable
    @Override
    public String getDestinationPartitionId(REQUEST request) {
        return request.getDestinationPartitionId();
    }

    @Override
    public List<String> getMessageHeader(REQUEST request, String name) {
        return request.getMessageHeader(name);
    }

    @Nullable
    @Override
    public String getSystem(REQUEST request) {
        return request.getSystem();
    }

    @Nullable
    @Override
    public String getDestination(REQUEST request) {
        return request.getDestination();
    }

    @Nullable
    @Override
    public String getDestinationTemplate(REQUEST request) {
        return request.getDestinationTemplate();
    }

    @Override
    public boolean isTemporaryDestination(REQUEST request) {
        return request.isTemporaryDestination();
    }

    @Override
    public boolean isAnonymousDestination(REQUEST request) {
        return request.isAnonymousDestination();
    }

    @Nullable
    @Override
    public String getConversationId(REQUEST request) {
        return request.getConversationId();
    }

    @Nullable
    @Override
    public Long getMessageBodySize(REQUEST request) {
        return request.getMessageBodySize();
    }

    @Nullable
    @Override
    public Long getMessageEnvelopeSize(REQUEST request) {
        return request.getMessageEnvelopeSize();
    }

    @Nullable
    @Override
    public String getMessageId(REQUEST request, @Nullable Void unused) {
        return request.getMessageId();
    }

    @Nullable
    @Override
    public String getClientId(REQUEST request) {
        return request.getClientId();
    }

    @Nullable
    @Override
    public Long getBatchMessageCount(REQUEST request, @Nullable Void unused) {
        return request.getBatchMessageCount();
    }
}
