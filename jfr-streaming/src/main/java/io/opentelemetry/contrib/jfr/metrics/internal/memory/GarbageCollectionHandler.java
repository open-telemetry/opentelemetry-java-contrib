/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jfr.metrics.internal.memory;

import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.ATTR_GC_CAUSE;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.ATTR_GC_COLLECTOR;
import static io.opentelemetry.contrib.jfr.metrics.internal.Constants.MILLISECONDS;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongHistogram;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.contrib.jfr.metrics.internal.RecordedEventHandler;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import jdk.jfr.consumer.RecordedEvent;

public class GarbageCollectionHandler implements RecordedEventHandler {
  private static final Logger logger = Logger.getLogger(GarbageCollectionHandler.class.getName());

  // FIXME Should this perhaps be time.stopped or time.pause?
  private static final String METRIC_NAME_DURATION = "process.runtime.jvm.gc.time";
  private static final String METRIC_DESCRIPTION_DURATION = "GC Duration";

  private static final String EVENT_NAME = "jdk.GarbageCollection";

  private static final String GC_ID = "gcId";
  private static final String GC_NAME = "name";
  private static final String GC_CAUSE = "cause";
  private static final String GC_PAUSES = "sumOfPauses";

  @SuppressWarnings("NullAway")
  private LongHistogram gcHistogram = null;

  private final ScheduledExecutorService reaper = Executors.newSingleThreadScheduledExecutor();
  // We require a CHM here to avoid dangerous races with the reaper thread
  private final Map<Long, LocalDateTime> seenIds = new ConcurrentHashMap<>();

  @Override
  public String getEventName() {
    return EVENT_NAME;
  }

  @Override
  public void initializeMeter(Meter meter) {
    gcHistogram =
        meter
            .histogramBuilder(METRIC_NAME_DURATION)
            .setDescription(METRIC_DESCRIPTION_DURATION)
            .setUnit(MILLISECONDS)
            .ofLongs()
            .build();

    // Reap every 30 mins
    Future<?> unusedFuture =
        reaper.scheduleAtFixedRate(() -> checkCache(), 30, 30, TimeUnit.MINUTES);
  }

  private void checkCache() {
    LocalDateTime startCheck = LocalDateTime.now(ZoneId.systemDefault());
    seenIds.forEach(
        (id, time) -> {
          if (time.isAfter(startCheck)) {
            seenIds.remove(id);
          }
        });
  }

  @Override
  public void accept(RecordedEvent ev) {
    if (!ev.hasField(GC_ID)) {
      logger.fine(String.format("GC Event seen without GC ID: %s", ev));
      return;
    }
    long gcId = ev.getLong(GC_ID);
    // FIXME Durations may be too short to record properly
    long duration = ev.getDuration(GC_PAUSES).toMillis();
    // NOTE: duration on this event type refers to the wall-clock time this GC ran for.
    // For a concurrent GC this gives a measure of how long the VM was running at reduced capacity
    String name = ev.getString(GC_NAME);
    String cause = ev.getString(GC_CAUSE);
    // FIXME What should "action" be?
    //    String action = "";

    Attributes attr = Attributes.of(ATTR_GC_COLLECTOR, name, ATTR_GC_CAUSE, cause);
    gcHistogram.record(duration, attr);

    // Add gcId to cached ids to time out in one hour
    seenIds.put(gcId, LocalDateTime.now(ZoneId.systemDefault()).plusHours(1));
  }
}
