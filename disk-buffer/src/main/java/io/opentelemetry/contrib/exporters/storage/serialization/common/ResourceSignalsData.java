package io.opentelemetry.contrib.exporters.storage.serialization.common;

import java.util.Collection;

public interface ResourceSignalsData<JSON_RESOURCE extends ResourceSignals<?>> {

    Collection<JSON_RESOURCE> getResourceSignals();
}
