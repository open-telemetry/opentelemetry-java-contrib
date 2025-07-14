/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.gcp.auth;

import static io.opentelemetry.contrib.gcp.auth.GcpAuthAutoConfigurationCustomizerProvider.SIGNAL_TYPE_METRICS;
import static io.opentelemetry.contrib.gcp.auth.GcpAuthAutoConfigurationCustomizerProvider.SIGNAL_TYPE_TRACES;
import static io.opentelemetry.contrib.gcp.auth.GcpAuthAutoConfigurationCustomizerProvider.shouldConfigureExporter;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.auto.service.AutoService;
import io.opentelemetry.contrib.sdk.autoconfigure.ConfigPropertiesUtil;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.extension.incubator.fileconfig.DeclarativeConfigurationCustomizer;
import io.opentelemetry.sdk.extension.incubator.fileconfig.DeclarativeConfigurationCustomizerProvider;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.BatchSpanProcessorModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.MeterProviderModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.MetricReaderModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.NameStringValuePairModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.OpenTelemetryConfigurationModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.OtlpGrpcExporterModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.OtlpGrpcMetricExporterModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.OtlpHttpExporterModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.OtlpHttpMetricExporterModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.PushMetricExporterModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.SimpleSpanProcessorModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.SpanExporterModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.SpanProcessorModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.TracerProviderModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

@AutoService(DeclarativeConfigurationCustomizerProvider.class)
public class GcpAuthCustomizerProvider implements DeclarativeConfigurationCustomizerProvider {

  static final String SIGNAL_TARGET_WARNING_YAML_FIX_SUGGESTION =
      String.format(
          "You may safely ignore this warning if it is intentional, otherwise please configure the '%s' by setting %s in the configuration file.",
          ConfigurableOption.GOOGLE_OTEL_AUTH_TARGET_SIGNALS.getUserReadableName(),
          ConfigPropertiesUtil.propertyYamlPath(
              ConfigurableOption.GOOGLE_OTEL_AUTH_TARGET_SIGNALS.getSystemProperty()));

  @Override
  public void customize(DeclarativeConfigurationCustomizer customizer) {
    customizer.addModelCustomizer(
        model -> {
          customize(
              model,
              GcpAuthAutoConfigurationCustomizerProvider.getCredentials(),
              ConfigPropertiesUtil.resolveModel(model));

          return model;
        });
  }

  static void customize(
      OpenTelemetryConfigurationModel model,
      GoogleCredentials credentials,
      ConfigProperties configProperties) {
    Map<String, String> headerMap =
        GcpAuthAutoConfigurationCustomizerProvider.getRequiredHeaderMap(
            credentials, configProperties);
    customizeMeter(model, headerMap, configProperties);
    customizeTracer(model, headerMap, configProperties);
  }

  private static void customizeMeter(
      OpenTelemetryConfigurationModel model,
      Map<String, String> headerMap,
      ConfigProperties configProperties) {
    MeterProviderModel meterProvider = model.getMeterProvider();
    if (meterProvider == null) {
      return;
    }

    if (shouldConfigureExporter(
        SIGNAL_TYPE_METRICS, SIGNAL_TARGET_WARNING_YAML_FIX_SUGGESTION, configProperties)) {
      for (MetricReaderModel reader : meterProvider.getReaders()) {
        if (reader.getPeriodic() != null) {
          addAuth(meterModelHeaders(reader.getPeriodic().getExporter()), headerMap);
        }
      }
    }
  }

  private static List<List<NameStringValuePairModel>> meterModelHeaders(
      @Nullable PushMetricExporterModel exporter) {
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

  private static void customizeTracer(
      OpenTelemetryConfigurationModel model,
      Map<String, String> headerMap,
      ConfigProperties configProperties) {
    TracerProviderModel tracerProvider = model.getTracerProvider();
    if (tracerProvider == null) {
      return;
    }

    if (shouldConfigureExporter(
        SIGNAL_TYPE_TRACES, SIGNAL_TARGET_WARNING_YAML_FIX_SUGGESTION, configProperties)) {
      for (SpanProcessorModel processor : tracerProvider.getProcessors()) {
        BatchSpanProcessorModel batch = processor.getBatch();
        if (batch != null) {
          addAuth(spanExporterModelHeaders(batch.getExporter()), headerMap);
        }
        SimpleSpanProcessorModel simple = processor.getSimple();
        if (simple != null) {
          addAuth(spanExporterModelHeaders(simple.getExporter()), headerMap);
        }
      }
    }
  }

  private static List<List<NameStringValuePairModel>> spanExporterModelHeaders(
      @Nullable SpanExporterModel exporter) {
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

  private static void addAuth(
      List<List<NameStringValuePairModel>> headerConsumers, Map<String, String> headerMap) {
    headerConsumers.forEach(headers -> addHeaders(headers, headerMap));
  }

  private static void addHeaders(List<NameStringValuePairModel> headers, Map<String, String> add) {
    add.forEach(
        (key, value) -> {
          if (headers.stream().noneMatch(header -> key.equals(header.getName()))) {
            headers.add(new NameStringValuePairModel().withName(key).withValue(value));
          }
        });
  }
}
