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
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.NETWORK_MODE_WRITE;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.BoundDoubleHistogram;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.contrib.jfr.metrics.internal.AbstractThreadDispatchingHandler;
import io.opentelemetry.contrib.jfr.metrics.internal.ThreadGrouper;
import java.util.function.Consumer;
import jdk.jfr.consumer.RecordedEvent;

// jdk.SocketWrite {
//        startTime = 20:22:57.161
//        duration = 87.4 ms
//        host = "mysql-staging-agentdb-2"
//        address = "10.31.1.15"
//        port = 3306
//        bytesWritten = 34 bytes
//        eventThread = "ActivityWriteDaemon" (javaThreadId = 252)
//        stackTrace = [
//        java.net.SocketOutputStream.socketWrite(byte[], int, int) line: 68
//        java.net.SocketOutputStream.write(byte[], int, int) line: 150
//        java.io.BufferedOutputStream.flushBuffer() line: 81
//        java.io.BufferedOutputStream.flush() line: 142
//        com.mysql.cj.protocol.a.SimplePacketSender.send(byte[], int, byte) line: 55
//        ...
//        ]
// }

public final class NetworkWriteHandler extends AbstractThreadDispatchingHandler {
  private static final String EVENT_NAME = "jdk.SocketWrite";

  private final DoubleHistogram bytesHistogram;
  private final DoubleHistogram durationHistogram;

  public NetworkWriteHandler(Meter otelMeter, ThreadGrouper nameNormalizer) {
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
    return new PerThreadNetworkWriteHandler(bytesHistogram, durationHistogram, threadName);
  }

  private static final class PerThreadNetworkWriteHandler implements Consumer<RecordedEvent> {
    private static final String BYTES_WRITTEN = "bytesWritten";

    private final BoundDoubleHistogram boundBytesHistogram;
    private final BoundDoubleHistogram boundDurationHistogram;

    private PerThreadNetworkWriteHandler(
        DoubleHistogram bytesHistogram, DoubleHistogram durationHistogram, String threadName) {
      var attr = Attributes.of(ATTR_THREAD_NAME, threadName, ATTR_NETWORK_MODE, NETWORK_MODE_WRITE);
      boundBytesHistogram = bytesHistogram.bind(attr);
      boundDurationHistogram = durationHistogram.bind(attr);
    }

    @Override
    public void accept(RecordedEvent ev) {
      boundBytesHistogram.record(ev.getLong(BYTES_WRITTEN));
      boundDurationHistogram.record(ev.getDuration().toMillis());
    }
  }
}
