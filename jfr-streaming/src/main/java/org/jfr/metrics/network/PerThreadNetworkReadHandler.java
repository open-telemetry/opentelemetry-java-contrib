package org.jfr.metrics.network;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.BoundDoubleHistogram;
import io.opentelemetry.api.metrics.Meter;
import jdk.jfr.consumer.RecordedEvent;
import org.jfr.metrics.RecordedEventHandler;

public class PerThreadNetworkReadHandler implements RecordedEventHandler {
  private final String threadName;

  public static final String JFR_SOCKET_READ_DURATION = "jfr.SocketRead.duration";
  public static final String JFR_SOCKET_READ_BYTES_READ = "jfr.SocketRead.bytesRead";
  public static final String BYTES_READ = "bytesRead";
  public static final String THREAD_NAME = "thread.name";
  private final Meter otelMeter;
  private BoundDoubleHistogram bytesHistogram;
  private BoundDoubleHistogram durationHistogram;

  public PerThreadNetworkReadHandler(Meter otelMeter, String threadName) {
    this.threadName = threadName;
    this.otelMeter = otelMeter;
  }

  public PerThreadNetworkReadHandler init() {
    var attr = Attributes.of(AttributeKey.stringKey(THREAD_NAME), threadName);

    var builder = otelMeter.histogramBuilder(JFR_SOCKET_READ_BYTES_READ);
    builder.setDescription("Bytes Read");
    builder.setUnit("KB");
    bytesHistogram = builder.build().bind(attr);

    builder = otelMeter.histogramBuilder(JFR_SOCKET_READ_DURATION);
    builder.setDescription("Read Duration");
    builder.setUnit("ms");
    durationHistogram = builder.build().bind(attr);

    return this;
  }

  @Override
  public String getEventName() {
    return NetworkReadHandler.EVENT_NAME;
  }

  @Override
  public void accept(RecordedEvent ev) {
    bytesHistogram.record(ev.getLong(BYTES_READ));
    durationHistogram.record(ev.getDuration().toMillis());
  }

}
