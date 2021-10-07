package org.jfr.metrics.cpu;

import io.opentelemetry.api.metrics.*;
import jdk.jfr.consumer.RecordedEvent;
import org.jfr.metrics.RecordedEventHandler;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.jfr.metrics.Constants.PERCENTAGE;

public class ContextSwitchRateHandler implements RecordedEventHandler {
  public static final String EVENT_NAME = "jdk.ThreadContextSwitchRate";

  private final Meter otelMeter;
  private volatile double value = 0;

  public ContextSwitchRateHandler(Meter otelMeter) {
    this.otelMeter = otelMeter;
  }

  public ContextSwitchRateHandler init() {
    otelMeter.upDownCounterBuilder("jfr.ThreadContextSwitchRate")
                                     .ofDoubles()
                                     .setUnit(PERCENTAGE)
                                     .buildWithCallback(codm -> codm.observe(value));
    return this;
  }

  @Override
  public void accept(RecordedEvent ev) {
    value = ev.getDouble("switchRate");
  }

  public String getEventName() {
    return EVENT_NAME;
  }

  @Override
  public Optional<Duration> getPollingDuration() {
    return Optional.of(Duration.of(1, ChronoUnit.SECONDS));
  }

}
