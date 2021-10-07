package io.opentelemetry.contrib.jfr.metrics;

import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedThread;

import java.util.Optional;

public class ThreadGrouper {
    // FIXME doesn't actually do any grouping, but should be safe for now
    public Optional<String> groupedName(RecordedEvent ev) {
        Object thisField = ev.getValue("eventThread");
        if (thisField != null && thisField instanceof RecordedThread) {
            return Optional.of(((RecordedThread) thisField).getJavaName());
        }
        return Optional.empty();
    }
}
