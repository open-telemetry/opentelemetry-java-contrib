/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jfr.metrics.internal.network;

import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.ATTR_NETWORK_MODE;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.ATTR_THREAD_NAME;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.BYTES;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.METRIC_DESCRIPTION_NETWORK_BYTES;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.METRIC_DESCRIPTION_NETWORK_DURATION;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.METRIC_NAME_NETWORK_BYTES;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.METRIC_NAME_NETWORK_DURATION;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.MILLISECONDS;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.NETWORK_MODE_READ;
import static io.opentelemetry.contrib.jfr.metrics.internal.RecordedEventHandler.defaultMeter;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.contrib.jfr.metrics.internal.AbstractThreadDispatchingHandler;
import io.opentelemetry.contrib.jfr.metrics.internal.ThreadGrouper;
import java.util.function.Consumer;
import jdk.jfr.consumer.RecordedEvent;

public final class NetworkReadHandler extends AbstractThreadDispatchingHandler {
  private static final String EVENT_NAME = "jdk.SocketRead";

  private DoubleHistogram bytesHistogram;
  private DoubleHistogram durationHistogram;

  public NetworkReadHandler(ThreadGrouper nameNormalizer) {
    super(nameNormalizer);
    initializeMeter(defaultMeter());
  }

  @Override
  public void initializeMeter(Meter meter) {
    bytesHistogram =
        meter
            .histogramBuilder(METRIC_NAME_NETWORK_BYTES)
            .setDescription(METRIC_DESCRIPTION_NETWORK_BYTES)
            .setUnit(BYTES)
            .build();
    durationHistogram =
        meter
            .histogramBuilder(METRIC_NAME_NETWORK_DURATION)
            .setDescription(METRIC_DESCRIPTION_NETWORK_DURATION)
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

    private final DoubleHistogram bytesHistogram;
    private final DoubleHistogram durationHistogram;
    private final Attributes attributes;

    public PerThreadNetworkReadHandler(
        DoubleHistogram bytesHistogram, DoubleHistogram durationHistogram, String threadName) {
      this.bytesHistogram = bytesHistogram;
      this.durationHistogram = durationHistogram;
      this.attributes =
          Attributes.of(ATTR_THREAD_NAME, threadName, ATTR_NETWORK_MODE, NETWORK_MODE_READ);
    }

    @Override
    public void accept(RecordedEvent ev) {
      bytesHistogram.record(ev.getLong(BYTES_READ), attributes);
      durationHistogram.record(ev.getDuration().toMillis(), attributes);
    }
  }
}
