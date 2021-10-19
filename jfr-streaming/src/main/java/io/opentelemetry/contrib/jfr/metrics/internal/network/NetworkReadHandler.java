/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jfr.metrics.internal.network;

import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.contrib.jfr.metrics.internal.AbstractThreadDispatchingHandler;
import io.opentelemetry.contrib.jfr.metrics.internal.RecordedEventHandler;
import io.opentelemetry.contrib.jfr.metrics.internal.ThreadGrouper;

public final class NetworkReadHandler extends AbstractThreadDispatchingHandler {
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
