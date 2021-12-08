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
    var meter =
        meterProvider
            .meterBuilder(INSTRUMENTATION_NAME)
            .setInstrumentationVersion(INSTRUMENTATION_VERSION)
            .build();
    var grouper = new ThreadGrouper();
    var handlers =
        List.of(
            new ObjectAllocationInNewTLABHandler(grouper),
            new ObjectAllocationOutsideTLABHandler(grouper),
            new NetworkReadHandler(grouper),
            new NetworkWriteHandler(grouper),
            new G1GarbageCollectionHandler(),
            new GCHeapSummaryHandler(),
            new ContextSwitchRateHandler(),
            new OverallCPULoadHandler(),
            new ContainerConfigurationHandler(),
            new LongLockHandler(grouper));
    handlers.forEach(handler -> handler.initializeMeter(meter));

    return new HandlerRegistry(handlers);
  }

  /** @return all entries in this registry. */
  List<RecordedEventHandler> all() {
    return mappers;
  }
}
