package io.opentelemetry.contrib.messaging.wrappers.mns;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.contrib.messaging.wrappers.MessagingProcessWrapper;
import io.opentelemetry.contrib.messaging.wrappers.MessagingSpanCustomizer;
import io.opentelemetry.contrib.messaging.wrappers.mns.semconv.MNSProcessRequest;
import io.opentelemetry.contrib.messaging.wrappers.mns.semconv.MNSProcessResponse;
import java.util.List;

public class MNSProcessWrapper<REQUEST extends MNSProcessRequest, RESPONSE extends MNSProcessResponse<?>>
        extends MessagingProcessWrapper<REQUEST, RESPONSE> {

    protected MNSProcessWrapper(OpenTelemetry openTelemetry,
                                TextMapGetter<REQUEST> textMapGetter,
                                List<MessagingSpanCustomizer<REQUEST, RESPONSE>> spanCustomizers) {
        super(openTelemetry, textMapGetter, spanCustomizers);
    }
}
