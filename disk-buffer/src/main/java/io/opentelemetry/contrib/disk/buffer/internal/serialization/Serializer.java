package io.opentelemetry.contrib.disk.buffer.internal.serialization;

import com.dslplatform.json.DslJson;
import com.dslplatform.json.JsonReader;
import com.dslplatform.json.JsonWriter;
import com.dslplatform.json.runtime.Settings;
import io.opentelemetry.api.common._Attributes_DslJsonConverter;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.models.common._ResourceJson_DslJsonConverter;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.models.common._ResourceSignals_DslJsonConverter;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.models.common._ScopeJson_DslJsonConverter;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.models.logs._BodyJson_DslJsonConverter;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.models.logs._LogRecordDataJson_DslJsonConverter;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.models.logs._ResourceLogsData_DslJsonConverter;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.models.logs._ResourceLogs_DslJsonConverter;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.models.logs._ScopeLogs_DslJsonConverter;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.models.metrics._MetricDataJson_DslJsonConverter;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.models.metrics._ResourceMetricsData_DslJsonConverter;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.models.metrics._ResourceMetrics_DslJsonConverter;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.models.metrics._ScopeMetrics_DslJsonConverter;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.models.metrics.data._DataJson_DslJsonConverter;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.models.metrics.data._ExponentialHistogram_DslJsonConverter;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.models.metrics.data._Gauge_DslJsonConverter;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.models.metrics.data._Histogram_DslJsonConverter;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.models.metrics.data._Sum_DslJsonConverter;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.models.metrics.data._Summary_DslJsonConverter;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.models.metrics.datapoints._ExponentialHistogramDataPoint_DslJsonConverter;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.models.metrics.datapoints._HistogramDataPoint_DslJsonConverter;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.models.metrics.datapoints._NumberDataPoint_DslJsonConverter;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.models.metrics.datapoints._SummaryDataPoint_DslJsonConverter;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.models.metrics.datapoints.data._Buckets_DslJsonConverter;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.models.metrics.datapoints.data._Exemplar_DslJsonConverter;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.models.metrics.datapoints.data._QuantileValue_DslJsonConverter;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.models.metrics.impl._ExponentialHistogramMetric_DslJsonConverter;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.models.metrics.impl._GaugeMetric_DslJsonConverter;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.models.metrics.impl._HistogramMetric_DslJsonConverter;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.models.metrics.impl._SumMetric_DslJsonConverter;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.models.metrics.impl._SummaryMetric_DslJsonConverter;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.models.spans._EventDataJson_DslJsonConverter;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.models.spans._LinkDataJson_DslJsonConverter;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.models.spans._ResourceSpansData_DslJsonConverter;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.models.spans._ResourceSpans_DslJsonConverter;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.models.spans._ScopeSpan_DslJsonConverter;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.models.spans._SpanDataJson_DslJsonConverter;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.models.spans._StatusDataJson_DslJsonConverter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public final class Serializer {

  private static final DslJson<Object> dslJson =
      new DslJson<>(
          Settings.withAnalyzers(/* unknownReader= */ false, /* unknownWriter= */ false)
              .skipDefaultValues(true)
              .with(new _Attributes_DslJsonConverter())
              .with(new _BodyJson_DslJsonConverter())
              .with(new _Buckets_DslJsonConverter())
              .with(new _DataJson_DslJsonConverter())
              .with(new _EventDataJson_DslJsonConverter())
              .with(new _Exemplar_DslJsonConverter())
              .with(new _ExponentialHistogramDataPoint_DslJsonConverter())
              .with(new _ExponentialHistogramMetric_DslJsonConverter())
              .with(new _ExponentialHistogram_DslJsonConverter())
              .with(new _GaugeMetric_DslJsonConverter())
              .with(new _Gauge_DslJsonConverter())
              .with(new _HistogramDataPoint_DslJsonConverter())
              .with(new _HistogramMetric_DslJsonConverter())
              .with(new _Histogram_DslJsonConverter())
              .with(new _LinkDataJson_DslJsonConverter())
              .with(new _LogRecordDataJson_DslJsonConverter())
              .with(new _MetricDataJson_DslJsonConverter())
              .with(new _NumberDataPoint_DslJsonConverter())
              .with(new _QuantileValue_DslJsonConverter())
              .with(new _ResourceJson_DslJsonConverter())
              .with(new _ResourceLogsData_DslJsonConverter())
              .with(new _ResourceLogs_DslJsonConverter())
              .with(new _ResourceMetricsData_DslJsonConverter())
              .with(new _ResourceMetrics_DslJsonConverter())
              .with(new _ResourceSignals_DslJsonConverter())
              .with(new _ResourceSpansData_DslJsonConverter())
              .with(new _ResourceSpans_DslJsonConverter())
              .with(new _ScopeJson_DslJsonConverter())
              .with(new _ScopeLogs_DslJsonConverter())
              .with(new _ScopeMetrics_DslJsonConverter())
              .with(new _ScopeSpan_DslJsonConverter())
              .with(new _SpanDataJson_DslJsonConverter())
              .with(new _StatusDataJson_DslJsonConverter())
              .with(new _SumMetric_DslJsonConverter())
              .with(new _Sum_DslJsonConverter())
              .with(new _SummaryDataPoint_DslJsonConverter())
              .with(new _SummaryMetric_DslJsonConverter())
              .with(new _Summary_DslJsonConverter()));

  private Serializer() {}

  public static <T> JsonReader.ReadObject<T> tryFindReader(Class<T> manifest) {
    return dslJson.tryFindReader(manifest);
  }

  public static <T> JsonWriter.WriteObject<T> tryFindWriter(Class<T> manifest) {
    return dslJson.tryFindWriter(manifest);
  }

  public static <T> T deserialize(Class<T> type, byte[] value) throws IOException {
    try (ByteArrayInputStream in = new ByteArrayInputStream(value)) {
      return dslJson.deserialize(type, in);
    }
  }

  public static byte[] serialize(Object object) throws IOException {
    try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      dslJson.serialize(object, out);
      return out.toByteArray();
    }
  }
}
