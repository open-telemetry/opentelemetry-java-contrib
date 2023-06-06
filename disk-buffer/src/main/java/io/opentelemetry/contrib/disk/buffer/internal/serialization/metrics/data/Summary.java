package io.opentelemetry.contrib.disk.buffer.internal.serialization.metrics.data;

import com.dslplatform.json.CompiledJson;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.metrics.datapoints.SummaryDataPoint;

@CompiledJson
public final class Summary extends DataJson<SummaryDataPoint> {}
