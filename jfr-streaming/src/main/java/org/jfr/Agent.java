package org.jfr;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporter;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.SdkMeterProviderBuilder;
import io.opentelemetry.sdk.metrics.common.InstrumentType;
import io.opentelemetry.sdk.metrics.export.IntervalMetricReader;
import io.opentelemetry.sdk.metrics.internal.aggregator.AggregatorFactory;
import io.opentelemetry.sdk.metrics.view.Aggregation;
import io.opentelemetry.sdk.metrics.view.InstrumentSelector;
import io.opentelemetry.sdk.metrics.view.View;
import io.opentelemetry.sdk.resources.Resource;
import jdk.jfr.EventSettings;
import jdk.jfr.consumer.RecordingStream;
import org.jfr.metrics.RecordedEventHandler;
import org.jfr.metrics.HandlerRegistry;

import java.lang.instrument.Instrumentation;
import java.util.Collections;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import static io.opentelemetry.sdk.metrics.common.InstrumentType.*;
import static io.opentelemetry.sdk.metrics.data.AggregationTemporality.DELTA;

public class Agent {
    private static final Logger logger = Logger.getLogger(Agent.class.getName());

    private static final String OTLP_URL = "OTLP_URL"; // https://otlp.nr-data.net:4317/ ; http://localhost:4317
    private static final String API_KEY = "API_KEY";

    private static SdkMeterProvider meterProvider;

    public static void premain(String agentArgs, Instrumentation inst) {
        configureOpenTelemetry();

        var jfrMonitorService = Executors.newSingleThreadExecutor();
        var toMetricRegistry = HandlerRegistry.createDefault(meterProvider);

        jfrMonitorService.submit(() -> {
            try (var recordingStream = new RecordingStream()) {
                var enableMappedEvent = eventEnablerFor(recordingStream);
                toMetricRegistry.all().forEach(enableMappedEvent);
                recordingStream.setReuse(false);
                logger.log(Level.FINE, "Starting recording stream...");
                recordingStream.start(); //run forever
            }
        });

    }

    private static Consumer<RecordedEventHandler> eventEnablerFor(RecordingStream recordingStream) {
        return handler -> {
            EventSettings eventSettings = recordingStream.enable(handler.getEventName());
            handler.getPollingDuration().ifPresent(eventSettings::withPeriod);
            recordingStream.onEvent(handler.getEventName(), handler);
        };
    }

    static void configureOpenTelemetry() {
        // Configure the meter provider
        var resource = Resource.getDefault().merge(
                Resource.create(Attributes.builder()
                        .put("service.name", "jfr-otlp-bridge")
                        .build()));
        var apiKey = System.getenv(API_KEY);
        var otlpUrl = System.getenv(OTLP_URL);
        if (otlpUrl == null) {
            otlpUrl = "http://localhost:4317";
        }

        var exporterBuilder = OtlpGrpcMetricExporter.builder()
                .setEndpoint(otlpUrl);

        if (apiKey != null) {
            exporterBuilder.addHeader("api-key", apiKey);
        }
        var meterProviderBuilder = SdkMeterProvider.builder().setResource(resource);
        // Set delta aggregation
        setAggregatorFactory(meterProviderBuilder, COUNTER, Aggregation.sum(DELTA));
        setAggregatorFactory(meterProviderBuilder, UP_DOWN_COUNTER, Aggregation.sum(DELTA));
        setAggregatorFactory(meterProviderBuilder, OBSERVABLE_SUM, Aggregation.histogram());
        setAggregatorFactory(meterProviderBuilder, OBSERVABLE_UP_DOWN_SUM, Aggregation.histogram());
        meterProvider = meterProviderBuilder.build();

        var imr = IntervalMetricReader.builder()
                .setExportIntervalMillis(2000)
                .setMetricExporter(exporterBuilder.build())
                .setMetricProducers(Collections.singletonList(meterProvider))
                .build();
        imr.startAndRegisterGlobal();
    }

    private static void setAggregatorFactory(SdkMeterProviderBuilder builder, InstrumentType instrumentType, Aggregation aggregation) {
        builder.registerView(InstrumentSelector.builder()
                        .setInstrumentType(instrumentType)
                        .build(),
                View.builder()
                        .setAggregation(aggregation)
                        .build());
    }
}
