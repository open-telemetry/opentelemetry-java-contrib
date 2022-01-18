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
import io.opentelemetry.contrib.jfr.metrics.internal.memory.G1HeapSummaryHandler;
import io.opentelemetry.contrib.jfr.metrics.internal.memory.GCHeapSummaryHandler;
import io.opentelemetry.contrib.jfr.metrics.internal.memory.ObjectAllocationInNewTLABHandler;
import io.opentelemetry.contrib.jfr.metrics.internal.memory.ObjectAllocationOutsideTLABHandler;
import io.opentelemetry.contrib.jfr.metrics.internal.memory.ParallelHeapSummaryHandler;
import io.opentelemetry.contrib.jfr.metrics.internal.network.NetworkReadHandler;
import io.opentelemetry.contrib.jfr.metrics.internal.network.NetworkWriteHandler;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

final class HandlerRegistry {
  private static final String INSTRUMENTATION_NAME = "io.opentelemetry.contrib.jfr";
  private static final String INSTRUMENTATION_VERSION = "1.7.0-SNAPSHOT";

  private final List<RecordedEventHandler> mappers;

  private static final Map<String, List<Supplier<RecordedEventHandler>>> HANDLERS_PER_GC =
      Map.of(
          "G1",
          List.of(G1HeapSummaryHandler::new),
          "Parallel",
          List.of(ParallelHeapSummaryHandler::new));

  private HandlerRegistry(List<? extends RecordedEventHandler> mappers) {
    this.mappers = new ArrayList<>(mappers);
  }

  static HandlerRegistry createDefault(MeterProvider meterProvider) {
    var handlers = new ArrayList<RecordedEventHandler>();
    var seen = new HashSet<String>();
    for (var bean : ManagementFactory.getGarbageCollectorMXBeans()) {
      var name = bean.getName();
      for (var gcType : HANDLERS_PER_GC.keySet()) {
        if (name.contains(gcType)
            && !seen.contains(gcType)
            && HANDLERS_PER_GC.get(gcType) != null) {
          handlers.addAll(HANDLERS_PER_GC.get(gcType).stream().map(s -> s.get()).toList());
          seen.add(gcType);
        }
      }
    }
    var grouper = new ThreadGrouper();
    var basicHandlers =
        List.of(
            new ObjectAllocationInNewTLABHandler(grouper),
            new ObjectAllocationOutsideTLABHandler(grouper),
            new NetworkReadHandler(grouper),
            new NetworkWriteHandler(grouper),
            new GCHeapSummaryHandler(),
            new ContextSwitchRateHandler(),
            new OverallCPULoadHandler(),
            new ContainerConfigurationHandler(),
            new LongLockHandler(grouper));
    handlers.addAll(basicHandlers);

    var meter =
        meterProvider
            .meterBuilder(INSTRUMENTATION_NAME)
            .setInstrumentationVersion(INSTRUMENTATION_VERSION)
            .build();
    handlers.forEach(handler -> handler.initializeMeter(meter));

    return new HandlerRegistry(handlers);
  }

  /** @return all entries in this registry. */
  List<RecordedEventHandler> all() {
    return mappers;
  }
}
