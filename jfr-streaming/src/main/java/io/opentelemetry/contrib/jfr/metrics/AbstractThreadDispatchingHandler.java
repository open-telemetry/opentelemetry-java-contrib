/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jfr.metrics;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import jdk.jfr.consumer.RecordedEvent;

public abstract class AbstractThreadDispatchingHandler implements RecordedEventHandler {
  // Will need pruning code for fast-cycling thread frameworks to prevent memory leaks
  protected final Map<String, RecordedEventHandler> perThread = new HashMap<>();
  protected final ThreadGrouper grouper;

  public AbstractThreadDispatchingHandler(ThreadGrouper grouper) {
    this.grouper = grouper;
  }

  public void reset() {
    perThread.clear();
  }

  public abstract String getEventName();

  public abstract RecordedEventHandler createPerThreadSummarizer(String threadName);

  @Override
  public void accept(RecordedEvent ev) {
    final Optional<String> possibleGroupedThreadName = grouper.groupedName(ev);
    possibleGroupedThreadName.ifPresent(
        groupedThreadName -> {
          if (perThread.get(groupedThreadName) == null) {
            perThread.put(groupedThreadName, createPerThreadSummarizer(groupedThreadName));
          }
          perThread.get(groupedThreadName).accept(ev);
        });
  }
}
