package io.opentelemetry.contrib.disk.buffer.internal.serialization.converters;

import com.dslplatform.json.JsonConverter;
import com.dslplatform.json.JsonReader;
import com.dslplatform.json.JsonWriter;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.Serializer;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.metrics.MetricDataJson;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.metrics.data.ExponentialHistogram;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.metrics.data.Gauge;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.metrics.data.Histogram;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.metrics.data.Sum;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.metrics.data.Summary;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.metrics.impl.ExponentialHistogramMetric;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.metrics.impl.GaugeMetric;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.metrics.impl.HistogramMetric;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.metrics.impl.SumMetric;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.metrics.impl.SummaryMetric;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@SuppressWarnings("unchecked")
@JsonConverter(target = MetricDataJson.class)
public final class MetricDataJsonConverter {
  private static final int METRIC_DATA_NUM_OF_KEYS = 4;
  private static final Map<Class<?>, JsonReader.ReadObject<?>> READERS = new HashMap<>();
  private static final Map<Class<?>, JsonWriter.WriteObject<?>> WRITERS = new HashMap<>();

  private MetricDataJsonConverter() {}

  public static MetricDataJson read(JsonReader<?> reader) throws IOException {
    String name = null;
    String description = null;
    String unit = null;
    MetricDataJson metricDataJson = null;
    for (int i = 0; i < METRIC_DATA_NUM_OF_KEYS; i++) {
      reader.getNextToken();
      String key = reader.readKey();
      switch (key) {
        case SumMetric.DATA_NAME:
          metricDataJson = new SumMetric();
          metricDataJson.setData(getReader(Sum.class).read(reader));
          break;
        case GaugeMetric.DATA_NAME:
          metricDataJson = new GaugeMetric();
          metricDataJson.setData(getReader(Gauge.class).read(reader));
          break;
        case HistogramMetric.DATA_NAME:
          metricDataJson = new HistogramMetric();
          metricDataJson.setData(getReader(Histogram.class).read(reader));
          break;
        case SummaryMetric.DATA_NAME:
          metricDataJson = new SummaryMetric();
          metricDataJson.setData(getReader(Summary.class).read(reader));
          break;
        case ExponentialHistogramMetric.DATA_NAME:
          metricDataJson = new ExponentialHistogramMetric();
          metricDataJson.setData(getReader(ExponentialHistogram.class).read(reader));
          break;
        case MetricDataJson.NAME:
          name = reader.readString();
          break;
        case MetricDataJson.DESCRIPTION:
          description = reader.readString();
          break;
        case MetricDataJson.UNIT:
          unit = reader.readString();
          break;
        default:
          throw new IllegalArgumentException();
      }
      reader.getNextToken();
    }
    Objects.requireNonNull(metricDataJson);
    Objects.requireNonNull(name);
    Objects.requireNonNull(description);
    Objects.requireNonNull(unit);
    metricDataJson.name = name;
    metricDataJson.description = description;
    metricDataJson.unit = unit;
    return metricDataJson;
  }

  public static void write(JsonWriter writer, MetricDataJson value) {
    if (value instanceof ExponentialHistogramMetric) {
      getWriter(ExponentialHistogramMetric.class).write(writer, (ExponentialHistogramMetric) value);
    } else if (value instanceof GaugeMetric) {
      getWriter(GaugeMetric.class).write(writer, (GaugeMetric) value);
    } else if (value instanceof HistogramMetric) {
      getWriter(HistogramMetric.class).write(writer, (HistogramMetric) value);
    } else if (value instanceof SummaryMetric) {
      getWriter(SummaryMetric.class).write(writer, (SummaryMetric) value);
    } else if (value instanceof SumMetric) {
      getWriter(SumMetric.class).write(writer, (SumMetric) value);
    } else {
      throw new UnsupportedOperationException();
    }
  }

  private static <T> JsonReader.ReadObject<T> getReader(Class<T> type) {
    JsonReader.ReadObject<?> reader = READERS.get(type);
    if (reader == null) {
      reader = Serializer.tryFindReader(type);
      READERS.put(type, reader);
    }

    return (JsonReader.ReadObject<T>) reader;
  }

  private static <T> JsonWriter.WriteObject<T> getWriter(Class<T> type) {
    JsonWriter.WriteObject<?> writer = WRITERS.get(type);
    if (writer == null) {
      writer = Serializer.tryFindWriter(type);
      WRITERS.put(type, writer);
    }

    return (JsonWriter.WriteObject<T>) writer;
  }
}
