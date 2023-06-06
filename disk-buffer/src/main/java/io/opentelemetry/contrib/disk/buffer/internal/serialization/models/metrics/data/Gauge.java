package io.opentelemetry.contrib.disk.buffer.internal.serialization.models.metrics.data;

import com.dslplatform.json.CompiledJson;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.models.metrics.datapoints.NumberDataPoint;

@CompiledJson
public final class Gauge extends DataJson<NumberDataPoint> {}
