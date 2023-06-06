package io.opentelemetry.contrib.exporters.storage.serialization.metrics.data;

import com.dslplatform.json.CompiledJson;
import io.opentelemetry.contrib.exporters.storage.serialization.metrics.datapoints.NumberDataPoint;

@CompiledJson
public final class Gauge extends DataJson<NumberDataPoint> {}
