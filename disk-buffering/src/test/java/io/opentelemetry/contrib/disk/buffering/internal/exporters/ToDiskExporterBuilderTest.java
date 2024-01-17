package io.opentelemetry.contrib.disk.buffering.internal.exporters;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.opentelemetry.contrib.disk.buffering.internal.StorageConfiguration;
import io.opentelemetry.sdk.trace.data.SpanData;
import org.junit.jupiter.api.Test;

class ToDiskExporterBuilderTest {

  @Test
  void whenMinFileReadIsNotGraterThanMaxFileWrite_throwException() {
    StorageConfiguration invalidConfig =
        StorageConfiguration.builder()
            .setMaxFileAgeForWriteMillis(2)
            .setMinFileAgeForReadMillis(1)
            .build();

    assertThatThrownBy(
        () -> ToDiskExporter.<SpanData>builder()
            .setStorageConfiguration(invalidConfig))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(
            "The configured max file age for writing must be lower than the configured min file age for reading");
  }
}
