/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jfr.metrics.internal;

import java.util.Optional;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedThread;

public final class ThreadGrouper {
  // FIXME doesn't actually do any grouping, but should be safe for now
  public Optional<String> groupedName(RecordedEvent ev) {
    Object thisField = ev.getValue("eventThread");
    if (thisField instanceof RecordedThread thread) {
      return Optional.of(thread.getJavaName());
    }
    return Optional.empty();
  }
}
