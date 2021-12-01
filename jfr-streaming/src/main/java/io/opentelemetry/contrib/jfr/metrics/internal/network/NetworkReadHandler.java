/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jfr.metrics.internal.network;

import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.ATTR_NETWORK_MODE;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.ATTR_THREAD_NAME;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.BYTES;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.MILLISECONDS;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.NETWORK_BYTES_DESCRIPTION;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.NETWORK_BYTES_NAME;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.NETWORK_DURATION_DESCRIPTION;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.NETWORK_DURATION_NAME;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.NETWORK_MODE_READ;

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
  private final DoubleHistogram bytesHistogram;
  private final DoubleHistogram durationHistogram;

  public NetworkReadHandler(Meter otelMeter, ThreadGrouper nameNormalizer) {
    super(nameNormalizer);
    bytesHistogram =
        otelMeter
            .histogramBuilder(NETWORK_BYTES_NAME)
            .setDescription(NETWORK_BYTES_DESCRIPTION)
            .setUnit(BYTES)
            .build();
    durationHistogram =
        otelMeter
            .histogramBuilder(NETWORK_DURATION_NAME)
            .setDescription(NETWORK_DURATION_DESCRIPTION)
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
      var attr = Attributes.of(ATTR_THREAD_NAME, threadName, ATTR_NETWORK_MODE, NETWORK_MODE_READ);
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
