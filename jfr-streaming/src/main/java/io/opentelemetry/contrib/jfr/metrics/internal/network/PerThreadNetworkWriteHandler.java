/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jfr.metrics.internal.network;

import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.ATTR_NETWORK_MODE;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.ATTR_THREAD_NAME;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.WRITE;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.BoundDoubleHistogram;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.contrib.jfr.metrics.internal.Constants;
import io.opentelemetry.contrib.jfr.metrics.internal.RecordedEventHandler;
import jdk.jfr.consumer.RecordedEvent;

public final class PerThreadNetworkWriteHandler implements RecordedEventHandler {
  private static final String SIMPLE_CLASS_NAME =
      PerThreadNetworkWriteHandler.class.getSimpleName();
  private static final String BYTES_WRITTEN = "bytesWritten";
  private static final String DURATION_METRIC_NAME = "runtime.jvm.network.duration";
  private static final String METRIC_NAME = "runtime.jvm.network.io";

  private static final String DESCRIPTION_BYTES = "Bytes Written";
  private static final String DESCRIPTION_DURATION = "Write Duration";

  private final String threadName;
  private final Meter otelMeter;

  private BoundDoubleHistogram bytesHistogram;
  private BoundDoubleHistogram durationHistogram;

  public PerThreadNetworkWriteHandler(Meter otelMeter, String threadName) {
    this.threadName = threadName;
    this.otelMeter = otelMeter;
  }

  public PerThreadNetworkWriteHandler init() {
    var attr = Attributes.of(ATTR_THREAD_NAME, threadName, ATTR_NETWORK_MODE, WRITE);

    var builder = otelMeter.histogramBuilder(METRIC_NAME);
    builder.setDescription(DESCRIPTION_BYTES);
    builder.setUnit(Constants.KILOBYTES);
    bytesHistogram = builder.build().bind(attr);

    builder = otelMeter.histogramBuilder(DURATION_METRIC_NAME);
    builder.setDescription(DESCRIPTION_DURATION);
    builder.setUnit(Constants.MILLISECONDS);
    durationHistogram = builder.build().bind(attr);

    return this;
  }

  @Override
  public String getEventName() {
    return NetworkWriteHandler.EVENT_NAME;
  }

  @Override
  public void accept(RecordedEvent ev) {
    bytesHistogram.record(ev.getLong(BYTES_WRITTEN));
    durationHistogram.record(ev.getDuration().toMillis());
  }
}
