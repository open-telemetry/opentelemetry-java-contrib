/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jfr.metrics.internal.network;

import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.ATTR_NETWORK_MODE;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.ATTR_THREAD_NAME;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.READ;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.BoundDoubleHistogram;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.contrib.jfr.metrics.internal.Constants;
import io.opentelemetry.contrib.jfr.metrics.internal.RecordedEventHandler;
import jdk.jfr.consumer.RecordedEvent;

public final class PerThreadNetworkReadHandler implements RecordedEventHandler {
  private static final String DESCRIPTION_BYTES = "Bytes Read";
  private static final String DESCRIPTION_DURATION = "Read Duration";
  private final String threadName;

  private static final String METRIC_NAME_DURATION = "runtime.jvm.network.duration";
  private static final String METRIC_NAME_BYTES = "runtime.jvm.network.io";
  private static final String BYTES_READ = "bytesRead";
  private final Meter otelMeter;
  private BoundDoubleHistogram bytesHistogram;
  private BoundDoubleHistogram durationHistogram;

  public PerThreadNetworkReadHandler(Meter otelMeter, String threadName) {
    this.threadName = threadName;
    this.otelMeter = otelMeter;
  }

  public PerThreadNetworkReadHandler init() {
    var attr = Attributes.of(ATTR_THREAD_NAME, threadName, ATTR_NETWORK_MODE, READ);

    var builder = otelMeter.histogramBuilder(METRIC_NAME_BYTES);
    builder.setDescription(DESCRIPTION_BYTES);
    builder.setUnit(Constants.BYTES);
    bytesHistogram = builder.build().bind(attr);

    builder = otelMeter.histogramBuilder(METRIC_NAME_DURATION);
    builder.setDescription(DESCRIPTION_DURATION);
    builder.setUnit(Constants.MILLISECONDS);
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
