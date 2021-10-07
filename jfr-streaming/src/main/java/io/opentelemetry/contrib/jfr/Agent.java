package io.opentelemetry.contrib.jfr;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporter;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.SdkMeterProviderBuilder;
import io.opentelemetry.sdk.metrics.common.InstrumentType;
import io.opentelemetry.sdk.metrics.view.Aggregation;
import io.opentelemetry.sdk.metrics.export.IntervalMetricReader;
import io.opentelemetry.sdk.metrics.view.InstrumentSelector;
import io.opentelemetry.sdk.metrics.view.View;
import io.opentelemetry.sdk.resources.Resource;
import jdk.jfr.EventSettings;
import jdk.jfr.consumer.RecordingStream;
import io.opentelemetry.contrib.jfr.metrics.RecordedEventHandler;
import io.opentelemetry.contrib.jfr.metrics.HandlerRegistry;

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

    private static final String OTLP_URL = "OTLP_URL";
    private static final String API_KEY = "API_KEY";
    private static final String SERVICE_NAME = "SERVICE_NAME";
    private static final String API_KEY_HEADER = "api-key";
    private static final String SERVICE_NAME_HEADER = "service.name";
    private static final String DEFAULT_URL = "http://localhost:4317";
    private static final String DEFAULT_SERVICE_NAME = "jfr-otlp-bridge";
    private static final long EXPORT_MILLIS = 2000;

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
        var serviceName = System.getenv(SERVICE_NAME);
        if (serviceName == null) {
            serviceName = DEFAULT_SERVICE_NAME;
        }
        var resource = Resource.getDefault().merge(
                Resource.create(Attributes.builder()
                        .put(SERVICE_NAME_HEADER, serviceName)
                        .build()));
        var apiKey = System.getenv(API_KEY);
        var otlpUrl = System.getenv(OTLP_URL);
        if (otlpUrl == null) {
            otlpUrl = DEFAULT_URL;
        }

        var exporterBuilder = OtlpGrpcMetricExporter.builder()
                .setEndpoint(otlpUrl);

        if (apiKey != null) {
            exporterBuilder.addHeader(API_KEY_HEADER, apiKey);
        }
        var meterProviderBuilder = SdkMeterProvider.builder().setResource(resource);
        // Set delta aggregation
        setAggregatorFactory(meterProviderBuilder, COUNTER, Aggregation.sum(DELTA));
        setAggregatorFactory(meterProviderBuilder, UP_DOWN_COUNTER, Aggregation.sum(DELTA));
        setAggregatorFactory(meterProviderBuilder, OBSERVABLE_SUM, Aggregation.histogram());
        setAggregatorFactory(meterProviderBuilder, OBSERVABLE_UP_DOWN_SUM, Aggregation.histogram());
        meterProvider = meterProviderBuilder.build();

        var imr = IntervalMetricReader.builder()
                .setExportIntervalMillis(EXPORT_MILLIS)
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
