package io.opentelemetry.contrib.exporters.storage.serialization.metrics.data;

import com.dslplatform.json.CompiledJson;
import io.opentelemetry.contrib.exporters.storage.serialization.metrics.datapoints.SummaryDataPoint;

@CompiledJson
public final class Summary extends DataJson<SummaryDataPoint> {}
