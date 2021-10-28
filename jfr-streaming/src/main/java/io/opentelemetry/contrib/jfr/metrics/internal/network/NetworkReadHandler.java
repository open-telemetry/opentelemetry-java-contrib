/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jfr.metrics.internal.network;

import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.ATTR_NETWORK_MODE;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.ATTR_THREAD_NAME;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.KILOBYTES;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.MILLISECONDS;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.READ;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.BoundDoubleHistogram;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.contrib.jfr.metrics.internal.AbstractThreadDispatchingHandler;
import io.opentelemetry.contrib.jfr.metrics.internal.ThreadGrouper;
import java.util.function.Consumer;
import jdk.jfr.consumer.RecordedEvent;

public final class NetworkReadHandler extends AbstractThreadDispatchingHandler {
  private static final String EVENT_NAME = "jdk.SocketRead";
  private static final String DESCRIPTION_BYTES = "Bytes Read";
  private static final String DESCRIPTION_DURATION = "Read Duration";
  private static final String METRIC_NAME_DURATION = "runtime.jvm.network.read.duration";
  private static final String METRIC_NAME_BYTES = "runtime.jvm.network.read.io";

  private final DoubleHistogram bytesHistogram;
  private final DoubleHistogram durationHistogram;

  public NetworkReadHandler(Meter otelMeter, ThreadGrouper nameNormalizer) {
    super(nameNormalizer);
    bytesHistogram =
        otelMeter
            .histogramBuilder(METRIC_NAME_BYTES)
            .setDescription(DESCRIPTION_BYTES)
            .setUnit(KILOBYTES)
            .build();
    durationHistogram =
        otelMeter
            .histogramBuilder(METRIC_NAME_DURATION)
            .setDescription(DESCRIPTION_DURATION)
            .setUnit(MILLISECONDS)
            .build();
  }

  @Override
  public String getEventName() {
    return EVENT_NAME;
  }

  @Override
  public Consumer<RecordedEvent> createPerThreadSummarizer(String threadName) {
    return new PerThreadNetworkReadHandler(bytesHistogram, durationHistogram, threadName);
  }

  private static class PerThreadNetworkReadHandler implements Consumer<RecordedEvent> {
    private static final String BYTES_READ = "bytesRead";

    private final BoundDoubleHistogram boundBytesHistogram;
    private final BoundDoubleHistogram boundDurationHistogram;

    public PerThreadNetworkReadHandler(
        DoubleHistogram bytesHistogram, DoubleHistogram durationHistogram, String threadName) {
      var attr = Attributes.of(ATTR_THREAD_NAME, threadName, ATTR_NETWORK_MODE, READ);
      this.boundBytesHistogram = bytesHistogram.bind(attr);
      this.boundDurationHistogram = durationHistogram.bind(attr);
    }

    @Override
    public void accept(RecordedEvent ev) {
      boundBytesHistogram.record(ev.getLong(BYTES_READ));
      boundDurationHistogram.record(ev.getDuration().toMillis());
    }
  }
}
