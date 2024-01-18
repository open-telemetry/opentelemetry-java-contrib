/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.exporter;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.opentelemetry.contrib.disk.buffering.internal.StorageConfiguration;
import io.opentelemetry.sdk.trace.data.SpanData;
import java.io.File;
import org.junit.jupiter.api.Test;

class ToDiskExporterBuilderTest {

  @Test
  void whenMinFileReadIsNotGraterThanMaxFileWrite_throwException() {
    StorageConfiguration invalidConfig =
        StorageConfiguration.builder()
            .setMaxFileAgeForWriteMillis(2)
            .setMinFileAgeForReadMillis(1)
            .setRootDir(new File("."))
            .build();

    assertThatThrownBy(
            () -> ToDiskExporter.<SpanData>builder().setStorageConfiguration(invalidConfig))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(
            "The configured max file age for writing must be lower than the configured min file age for reading");
  }
}
