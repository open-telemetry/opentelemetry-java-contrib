package org.jfr.metrics.network;

import io.opentelemetry.api.metrics.Meter;
import org.jfr.metrics.AbstractThreadDispatchingHandler;
import org.jfr.metrics.RecordedEventHandler;
import org.jfr.metrics.ThreadGrouper;

public class NetworkReadHandler extends AbstractThreadDispatchingHandler {
  public static final String EVENT_NAME = "jdk.SocketRead";
  private final Meter otelMeter;

  public NetworkReadHandler(Meter otelMeter, ThreadGrouper nameNormalizer) {
    super(nameNormalizer);
    this.otelMeter = otelMeter;
  }

  @Override
  public String getEventName() {
    return EVENT_NAME;
  }

  @Override
  public RecordedEventHandler createPerThreadSummarizer(String threadName) {
    var ret = new PerThreadNetworkReadHandler(otelMeter, threadName);
    return ret.init();
  }
}
