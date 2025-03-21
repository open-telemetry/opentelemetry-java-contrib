package io.opentelemetry.contrib.messaging.wrappers.mns;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.contrib.messaging.wrappers.MessagingSpanCustomizer;
import io.opentelemetry.contrib.messaging.wrappers.mns.semconv.MNSProcessRequest;
import io.opentelemetry.contrib.messaging.wrappers.mns.semconv.MNSProcessResponse;
import io.opentelemetry.contrib.messaging.wrappers.semconv.DefaultMessagingProcessSpanCustomizer;
import java.util.ArrayList;
import java.util.List;

public class MNSProcessWrapperBuilder<REQUEST extends MNSProcessRequest, RESPONSE extends MNSProcessResponse<?>> {

    private OpenTelemetry openTelemetry;

    private TextMapGetter<REQUEST> textMapGetter;

    private List<MessagingSpanCustomizer<REQUEST, RESPONSE>> spanCustomizers;

    public static <REQUEST extends MNSProcessRequest, RESPONSE extends MNSProcessResponse<?>> MNSProcessWrapperBuilder<REQUEST, RESPONSE> create() {
        return new MNSProcessWrapperBuilder<>();
    }

    public MNSProcessWrapperBuilder<REQUEST, RESPONSE> openTelemetry(OpenTelemetry openTelemetry) {
        this.openTelemetry = openTelemetry;
        return this;
    }

    public MNSProcessWrapperBuilder<REQUEST, RESPONSE> textMapGetter(TextMapGetter<REQUEST> textMapGetter) {
        this.textMapGetter = textMapGetter;
        return this;
    }

    public MNSProcessWrapperBuilder<REQUEST, RESPONSE> spanCustomizers(
            List<MessagingSpanCustomizer<REQUEST, RESPONSE>> spanCustomizers) {
        this.spanCustomizers = spanCustomizers;
        return this;
    }

    public MNSProcessWrapperBuilder<REQUEST, RESPONSE> addSpanCustomizers(
            MessagingSpanCustomizer<REQUEST, RESPONSE> spanCustomizer) {
        this.spanCustomizers.add(spanCustomizer);
        return this;
    }

    public MNSProcessWrapper<REQUEST, RESPONSE> build() {
        return new MNSProcessWrapper<>(this.openTelemetry, this.textMapGetter, this.spanCustomizers);
    }

    private MNSProcessWrapperBuilder() {
        // init by default
        this.openTelemetry = GlobalOpenTelemetry.get();
        this.spanCustomizers = new ArrayList<>();
        this.spanCustomizers.add(DefaultMessagingProcessSpanCustomizer.create());
        this.textMapGetter = MNSTextMapGetter.create();
    }
}
