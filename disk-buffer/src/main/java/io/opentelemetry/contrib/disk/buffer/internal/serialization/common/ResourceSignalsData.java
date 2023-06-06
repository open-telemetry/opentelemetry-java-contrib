package io.opentelemetry.contrib.disk.buffer.internal.serialization.common;

import java.util.Collection;

public interface ResourceSignalsData<JSON_RESOURCE extends ResourceSignals<?>> {

    Collection<JSON_RESOURCE> getResourceSignals();
}
