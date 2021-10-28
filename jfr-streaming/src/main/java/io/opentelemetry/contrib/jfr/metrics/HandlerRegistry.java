/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jfr.metrics;

import io.opentelemetry.api.metrics.MeterProvider;
import io.opentelemetry.contrib.jfr.metrics.internal.RecordedEventHandler;
import io.opentelemetry.contrib.jfr.metrics.internal.ThreadGrouper;
import io.opentelemetry.contrib.jfr.metrics.internal.container.ContainerConfigurationHandler;
import io.opentelemetry.contrib.jfr.metrics.internal.cpu.ContextSwitchRateHandler;
import io.opentelemetry.contrib.jfr.metrics.internal.cpu.LongLockHandler;
import io.opentelemetry.contrib.jfr.metrics.internal.cpu.OverallCPULoadHandler;
import io.opentelemetry.contrib.jfr.metrics.internal.memory.G1GarbageCollectionHandler;
import io.opentelemetry.contrib.jfr.metrics.internal.memory.GCHeapSummaryHandler;
import io.opentelemetry.contrib.jfr.metrics.internal.memory.ObjectAllocationInNewTLABHandler;
import io.opentelemetry.contrib.jfr.metrics.internal.memory.ObjectAllocationOutsideTLABHandler;
import io.opentelemetry.contrib.jfr.metrics.internal.network.NetworkReadHandler;
import io.opentelemetry.contrib.jfr.metrics.internal.network.NetworkWriteHandler;
import java.util.*;

final class HandlerRegistry {
  private static final String INSTRUMENTATION_NAME = "io.opentelemetry.contrib.jfr";
  private static final String INSTRUMENTATION_VERSION = "1.7.0-SNAPSHOT";

  private final List<RecordedEventHandler> mappers;

  private HandlerRegistry(List<? extends RecordedEventHandler> mappers) {
    this.mappers = new ArrayList<>(mappers);
  }

  static HandlerRegistry createDefault(MeterProvider meterProvider) {
    var otelMeter =
        meterProvider
            .meterBuilder(INSTRUMENTATION_NAME)
            .setInstrumentationVersion(INSTRUMENTATION_VERSION)
            .build();
    var grouper = new ThreadGrouper();
    return new HandlerRegistry(
        List.of(
            new ObjectAllocationInNewTLABHandler(otelMeter, grouper),
            new ObjectAllocationOutsideTLABHandler(otelMeter, grouper),
            new NetworkReadHandler(otelMeter, grouper),
            new NetworkWriteHandler(otelMeter, grouper),
            new G1GarbageCollectionHandler(otelMeter),
            new GCHeapSummaryHandler(otelMeter),
            new ContextSwitchRateHandler(otelMeter),
            new OverallCPULoadHandler(otelMeter),
            new ContainerConfigurationHandler(otelMeter),
            new LongLockHandler(otelMeter, grouper)));
  }

  /** @return all entries in this registry. */
  List<RecordedEventHandler> all() {
    return mappers;
  }
}
