/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.interceptor;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.common.Value;
import io.opentelemetry.api.logs.Logger;
import io.opentelemetry.api.logs.Severity;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.contrib.interceptor.common.ComposableInterceptor;
import io.opentelemetry.sdk.common.InstrumentationScopeInfo;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.data.Body;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.logs.export.SimpleLogRecordProcessor;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.testing.exporter.InMemoryLogRecordExporter;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InterceptableLogRecordExporterTest {
  private InMemoryLogRecordExporter memoryLogRecordExporter;
  private Logger logger;
  private ComposableInterceptor<LogRecordData> interceptor;

  @BeforeEach
  void setUp() {
    memoryLogRecordExporter = InMemoryLogRecordExporter.create();
    interceptor = new ComposableInterceptor<>();
    logger =
        SdkLoggerProvider.builder()
            .addLogRecordProcessor(
                SimpleLogRecordProcessor.create(
                    new InterceptableLogRecordExporter(memoryLogRecordExporter, interceptor)))
            .build()
            .get("TestScope");
  }

  @Test
  void verifyLogModification() {
    interceptor.add(
        item -> {
          ModifiableLogRecordData modified = new ModifiableLogRecordData(item);
          modified.attributes.put("global.attr", "from interceptor");
          return modified;
        });

    logger
        .logRecordBuilder()
        .setBody("One log")
        .setAttribute(AttributeKey.stringKey("local.attr"), "local")
        .emit();

    List<LogRecordData> finishedLogRecordItems =
        memoryLogRecordExporter.getFinishedLogRecordItems();
    assertThat(finishedLogRecordItems.size()).isEqualTo(1);
    LogRecordData logRecordData = finishedLogRecordItems.get(0);
    assertThat(logRecordData.getAttributes().size()).isEqualTo(2);
    assertThat(logRecordData.getAttributes().get(AttributeKey.stringKey("global.attr")))
        .isEqualTo("from interceptor");
    assertThat(logRecordData.getAttributes().get(AttributeKey.stringKey("local.attr")))
        .isEqualTo("local");
  }

  @Test
  void verifyLogFiltering() {
    interceptor.add(
        item -> {
          if (Objects.requireNonNull(item.getBodyValue()).asString().contains("deleted")) {
            return null;
          }
          return item;
        });

    logger.logRecordBuilder().setBody("One log").emit();
    logger.logRecordBuilder().setBody("This log will be deleted").emit();
    logger.logRecordBuilder().setBody("Another log").emit();

    List<LogRecordData> finishedLogRecordItems =
        memoryLogRecordExporter.getFinishedLogRecordItems();
    assertThat(finishedLogRecordItems.size()).isEqualTo(2);
    assertThat(finishedLogRecordItems.get(0).getBodyValue()).isEqualTo(Value.of("One log"));
    assertThat(finishedLogRecordItems.get(1).getBodyValue()).isEqualTo(Value.of("Another log"));
  }

  private static class ModifiableLogRecordData implements LogRecordData {
    private final LogRecordData delegate;
    private final AttributesBuilder attributes = Attributes.builder();

    private ModifiableLogRecordData(LogRecordData delegate) {
      this.delegate = delegate;
    }

    @Override
    public Resource getResource() {
      return delegate.getResource();
    }

    @Override
    public InstrumentationScopeInfo getInstrumentationScopeInfo() {
      return delegate.getInstrumentationScopeInfo();
    }

    @Override
    public long getTimestampEpochNanos() {
      return delegate.getTimestampEpochNanos();
    }

    @Override
    public long getObservedTimestampEpochNanos() {
      return delegate.getObservedTimestampEpochNanos();
    }

    @Override
    public SpanContext getSpanContext() {
      return delegate.getSpanContext();
    }

    @Override
    public Severity getSeverity() {
      return delegate.getSeverity();
    }

    @Nullable
    @Override
    public String getSeverityText() {
      return delegate.getSeverityText();
    }

    @Override
    @SuppressWarnings("deprecation") // implement deprecated method
    public Body getBody() {
      return delegate.getBody();
    }

    @Override
    public Attributes getAttributes() {
      return attributes.putAll(delegate.getAttributes()).build();
    }

    @Override
    public int getTotalAttributeCount() {
      return delegate.getTotalAttributeCount();
    }
  }
}
