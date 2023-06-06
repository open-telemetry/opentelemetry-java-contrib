package io.opentelemetry.contrib.disk.buffer.serialization.metrics.data;

import com.dslplatform.json.CompiledJson;
import io.opentelemetry.contrib.disk.buffer.serialization.metrics.datapoints.NumberDataPoint;

@CompiledJson
public final class Gauge extends DataJson<NumberDataPoint> {}
