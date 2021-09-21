package org.jfr;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporter;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.export.IntervalMetricReader;
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

public class Agent {
    private static final Logger logger = Logger.getLogger(Agent.class.getName());

    private static final String OTLP_URL = "http://localhost:4317";

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
        Resource resource = Resource.getDefault().merge(
                Resource.create(Attributes.builder()
                        .put("service.name", "jfr-otlp-bridge")
                        .build()));

        meterProvider = SdkMeterProvider.builder().setResource(resource).build();
        IntervalMetricReader imr = IntervalMetricReader.builder()
                .setExportIntervalMillis(2000)
                .setMetricExporter(OtlpGrpcMetricExporter.builder()
                        .setEndpoint(OTLP_URL)
                        .build())
                .setMetricProducers(Collections.singletonList(meterProvider))
                .build();
        imr.startAndRegisterGlobal();
    }
}
