/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jfr.metrics;

import io.opentelemetry.api.metrics.MeterProvider;
import io.opentelemetry.contrib.jfr.metrics.internal.GarbageCollection.G1GarbageCollectionHandler;
import io.opentelemetry.contrib.jfr.metrics.internal.GarbageCollection.OldGarbageCollectionHandler;
import io.opentelemetry.contrib.jfr.metrics.internal.GarbageCollection.YoungGarbageCollectionHandler;
import io.opentelemetry.contrib.jfr.metrics.internal.RecordedEventHandler;
import io.opentelemetry.contrib.jfr.metrics.internal.ThreadGrouper;
import io.opentelemetry.contrib.jfr.metrics.internal.classes.ClassesLoadedHandler;
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
import io.opentelemetry.contrib.jfr.metrics.internal.threads.ThreadCountHandler;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

final class HandlerRegistry {
  private static final String INSTRUMENTATION_NAME = "io.opentelemetry.contrib.jfr";
  private static final String INSTRUMENTATION_VERSION = "1.7.0-SNAPSHOT";

  private final List<RecordedEventHandler> mappers;

  private HandlerRegistry(List<? extends RecordedEventHandler> mappers) {
    this.mappers = new ArrayList<>(mappers);
  }

  static HandlerRegistry createDefault(MeterProvider meterProvider) {
    var handlers = new ArrayList<RecordedEventHandler>();

    for (var bean : ManagementFactory.getGarbageCollectorMXBeans()) {
      var name = bean.getName();
      switch (name) {
        case "G1 Young Generation":
          handlers.add(new G1HeapSummaryHandler());
          handlers.add(new G1GarbageCollectionHandler());
          break;

        case "Copy":
          handlers.add(new YoungGarbageCollectionHandler(name));
          break;

        case "PS Scavenge":
          handlers.add(new YoungGarbageCollectionHandler(name));
          handlers.add(new ParallelHeapSummaryHandler());
          break;

        case "G1 Old Generation":
          handlers.add(new OldGarbageCollectionHandler(name));
          break;

        case "PS MarkSweep":
          handlers.add(new OldGarbageCollectionHandler(name));
          break;

        case "MarkSweepCompact":
          handlers.add(new OldGarbageCollectionHandler(name));
          break;

        default:
          // If none of the above GCs are detected, no action.
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
            new LongLockHandler(grouper),
            new ThreadCountHandler(),
            new ClassesLoadedHandler());
    handlers.addAll(basicHandlers);

    var meter =
        meterProvider
            .meterBuilder(INSTRUMENTATION_NAME)
            .setInstrumentationVersion(INSTRUMENTATION_VERSION)
            .build();
    handlers.forEach(handler -> handler.initializeMeter(meter));

    return new HandlerRegistry(handlers);
  }

  /**
   * @return all entries in this registry.
   */
  List<RecordedEventHandler> all() {
    return mappers;
  }
}
