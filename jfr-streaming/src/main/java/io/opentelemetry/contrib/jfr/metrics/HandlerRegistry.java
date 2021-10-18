/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jfr.metrics;

import io.opentelemetry.api.metrics.MeterProvider;
import io.opentelemetry.contrib.jfr.metrics.container.ContainerConfigurationHandler;
import io.opentelemetry.contrib.jfr.metrics.cpu.ContextSwitchRateHandler;
import io.opentelemetry.contrib.jfr.metrics.cpu.LongLockHandler;
import io.opentelemetry.contrib.jfr.metrics.cpu.OverallCPULoadHandler;
import io.opentelemetry.contrib.jfr.metrics.memory.G1GarbageCollectionHandler;
import io.opentelemetry.contrib.jfr.metrics.memory.GCHeapSummaryHandler;
import io.opentelemetry.contrib.jfr.metrics.memory.ObjectAllocationInNewTLABHandler;
import io.opentelemetry.contrib.jfr.metrics.memory.ObjectAllocationOutsideTLABHandler;
import io.opentelemetry.contrib.jfr.metrics.network.NetworkReadHandler;
import io.opentelemetry.contrib.jfr.metrics.network.NetworkWriteHandler;
import java.util.*;
import java.util.stream.Stream;

public class HandlerRegistry {
  private static final String SCHEMA_URL = "https://opentelemetry.io/schemas/1.6.1";
  private static final String INSTRUMENTATION_NAME = "io.opentelemetry.contrib.jfr";
  private static final String INSTRUMENTATION_VERSION = "1.7.0-SNAPSHOT";

  private final List<RecordedEventHandler> mappers;

  private HandlerRegistry(List<? extends RecordedEventHandler> mappers) {
    this.mappers = new ArrayList<>(mappers);
  }

  public static HandlerRegistry createDefault(MeterProvider meterProvider) {
    var otelMeter = meterProvider.get(INSTRUMENTATION_NAME, INSTRUMENTATION_VERSION, null);

    var grouper = new ThreadGrouper();
    var filtered =
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
            new LongLockHandler(otelMeter, grouper));
    filtered.forEach(RecordedEventHandler::init);

    return new HandlerRegistry(filtered);
  }

  /** @return a stream of all entries in this registry. */
  public Stream<RecordedEventHandler> all() {
    return mappers.stream();
  }
}
