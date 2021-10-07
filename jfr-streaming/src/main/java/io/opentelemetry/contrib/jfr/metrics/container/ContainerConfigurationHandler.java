package io.opentelemetry.contrib.jfr.metrics.container;

import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.contrib.jfr.Constants;
import io.opentelemetry.contrib.jfr.metrics.RecordedEventHandler;
import jdk.jfr.consumer.RecordedEvent;

public class ContainerConfigurationHandler implements RecordedEventHandler {
    public static final String EVENT_NAME = "jdk.ContainerConfiguration";
    public static final String JFR_CONTAINER_CONFIGURATION = "jfr.ContainerConfiguration";

    private static final String EFFECTIVE_CPU_COUNT = "effectiveCpuCount";

    private final Meter otelMeter;
    private volatile long value = 0L;


    public ContainerConfigurationHandler(Meter otelMeter) {
        this.otelMeter = otelMeter;
    }

    public ContainerConfigurationHandler init() {
        otelMeter.upDownCounterBuilder(JFR_CONTAINER_CONFIGURATION)
                .ofDoubles()
                .setUnit(Constants.PERCENTAGE)
                .buildWithCallback(codm -> codm.observe(value));

        return this;
    }

        @Override
    public String getEventName() {
        return EVENT_NAME;
    }

    @Override
    public void accept(RecordedEvent ev) {
        if (ev.hasField(EFFECTIVE_CPU_COUNT)) {
            value = ev.getLong(EFFECTIVE_CPU_COUNT);
        }
    }
}
