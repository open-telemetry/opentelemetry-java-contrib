package io.opentelemetry.contrib.messaging.wrappers.mns.semconv;

import io.opentelemetry.contrib.messaging.wrappers.semconv.MessagingProcessResponse;

public class MNSProcessResponse<T> implements MessagingProcessResponse<T> {

    private final T originalResponse;

    public static <T> MNSProcessResponse<T> of(T originalResponse) {
        return new MNSProcessResponse<>(originalResponse);
    }

    @Override
    public T getOriginalResponse() {
        return originalResponse;
    }

    public MNSProcessResponse(T originalResponse) {
        this.originalResponse = originalResponse;
    }
}
