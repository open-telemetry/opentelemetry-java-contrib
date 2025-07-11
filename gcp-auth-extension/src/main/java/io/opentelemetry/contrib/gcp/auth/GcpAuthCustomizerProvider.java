package io.opentelemetry.contrib.gcp.auth;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.auto.service.AutoService;
import io.opentelemetry.sdk.extension.incubator.fileconfig.DeclarativeConfigurationCustomizer;
import io.opentelemetry.sdk.extension.incubator.fileconfig.DeclarativeConfigurationCustomizerProvider;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.BatchLogRecordProcessorModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.BatchSpanProcessorModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.LogRecordExporterModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.LogRecordProcessorModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.LoggerProviderModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.MeterProviderModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.MetricReaderModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.NameStringValuePairModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.OpenTelemetryConfigurationModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.OtlpGrpcExporterModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.OtlpGrpcMetricExporterModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.OtlpHttpExporterModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.OtlpHttpMetricExporterModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.PushMetricExporterModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.SimpleLogRecordProcessorModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.SimpleSpanProcessorModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.SpanExporterModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.SpanProcessorModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.TracerProviderModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@AutoService(DeclarativeConfigurationCustomizerProvider.class)
public class GcpAuthCustomizerProvider implements DeclarativeConfigurationCustomizerProvider {

  @Override
  public void customize(DeclarativeConfigurationCustomizer customizer) {
    customizer.addModelCustomizer(
        model -> {
          GoogleCredentials credentials = GcpAuthAutoConfigurationCustomizerProvider.getCredentials();
          // todo pass config bridge
          Map<String, String> headerMap = GcpAuthAutoConfigurationCustomizerProvider.getRequiredHeaderMap(
              credentials, null);
          customizeMeter(model, headerMap);
          // todo are loggers supported now (not covered in old variant)?
          customizeLogger(model, headerMap);
          customizeTracer(model, headerMap);

          return model;
        });
  }

  private void customizeMeter(OpenTelemetryConfigurationModel model,
      Map<String, String> headerMap) {
    MeterProviderModel meterProvider = model.getMeterProvider();
    if (meterProvider == null) {
      return;
    }

    for (MetricReaderModel reader : meterProvider.getReaders()) {
      if (reader.getPeriodic() != null) {
        addAuth(meterModelHeaders(reader.getPeriodic().getExporter()),
            headerMap);
      }
    }
  }

  private List<List<NameStringValuePairModel>> meterModelHeaders(
      PushMetricExporterModel exporter) {
    ArrayList<List<NameStringValuePairModel>> list = new ArrayList<>();
    if (exporter == null) {
      return list;
    }
    OtlpGrpcMetricExporterModel grpc = exporter.getOtlpGrpc();
    if (grpc != null) {
      list.add(grpc.getHeaders());
    }
    OtlpHttpMetricExporterModel http = exporter.getOtlpHttp();
    if (http != null) {
      list.add(http.getHeaders());
    }
    return list;
  }

  private void customizeLogger(OpenTelemetryConfigurationModel model,
      Map<String, String> headerMap) {
    LoggerProviderModel loggerProvider = model.getLoggerProvider();
    if (loggerProvider == null) {
      return;
    }
    for (LogRecordProcessorModel processor : loggerProvider.getProcessors()) {
      BatchLogRecordProcessorModel batch = processor.getBatch();
      if (batch != null) {
        addAuth(logRecordModelHeaders(batch.getExporter()),
            headerMap);
      }
      SimpleLogRecordProcessorModel simple = processor.getSimple();
      if (simple != null) {
        addAuth(logRecordModelHeaders(simple.getExporter()),
            headerMap);
      }
    }
  }

  private List<List<NameStringValuePairModel>> logRecordModelHeaders(
      LogRecordExporterModel exporter) {
    ArrayList<List<NameStringValuePairModel>> list = new ArrayList<>();

    if (exporter == null) {
      return list;
    }
    OtlpGrpcExporterModel grpc = exporter.getOtlpGrpc();
    if (grpc != null) {
      list.add(grpc.getHeaders());
    }
    OtlpHttpExporterModel http = exporter.getOtlpHttp();
    if (http != null) {
      list.add(http.getHeaders());
    }
    return list;
  }

  private void customizeTracer(OpenTelemetryConfigurationModel model,
      Map<String, String> headerMap) {
    TracerProviderModel tracerProvider = model.getTracerProvider();
    if (tracerProvider == null) {
      return;
    }

    // todo here we would want a simplified version of the declarative config bridge
    // https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/main/javaagent-extension-api/src/main/java/io/opentelemetry/javaagent/extension/DeclarativeConfigPropertiesBridge.java
//    googleNode(model)

//    if (!isSignalTargeted(SIGNAL_TYPE_TRACES, configProperties)) {
      // todo
//        String[] params = {SIGNAL_TYPE_TRACES, SIGNAL_TARGET_WARNING_FIX_SUGGESTION};
//        logger.log(
//            Level.WARNING,
//            "GCP Authentication Extension is not configured for signal type: {0}. {1}",
//            params);
//      return;
//    }

    for (SpanProcessorModel processor : tracerProvider.getProcessors()) {
      BatchSpanProcessorModel batch = processor.getBatch();
      if (batch != null) {
        addAuth(spanExporterModelHeaders(batch.getExporter()),
            headerMap);
      }
      SimpleSpanProcessorModel simple = processor.getSimple();
      if (simple != null) {
        addAuth(spanExporterModelHeaders(simple.getExporter()),
            headerMap);
      }
    }
  }

  private void googleNode(OpenTelemetryConfigurationModel model) {
    // todo use declarative config bridge
  }

  private List<List<NameStringValuePairModel>> spanExporterModelHeaders(
      SpanExporterModel exporter) {
    ArrayList<List<NameStringValuePairModel>> list = new ArrayList<>();

    if (exporter == null) {
      return list;
    }
    OtlpGrpcExporterModel grpc = exporter.getOtlpGrpc();
    if (grpc != null) {
      list.add(grpc.getHeaders());
    }
    OtlpHttpExporterModel http = exporter.getOtlpHttp();
    if (http != null) {
      list.add(http.getHeaders());
    }
    return list;
  }

  private void addAuth(
      List<List<NameStringValuePairModel>> headerConsumers, Map<String, String> headerMap) {
    headerConsumers.forEach(
        headers -> addHeaders(headers, headerMap));
  }

  private void addHeaders(List<NameStringValuePairModel> headers, Map<String, String> add) {
    add.forEach(
        (key, value) -> {
          if (headers.stream().noneMatch(header -> key.equals(header.getName()))) {
            headers.add(new NameStringValuePairModel().withName(key).withValue(value));
          }
        });
  }
}
