/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.exporters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import io.opentelemetry.contrib.disk.buffering.internal.storage.TestData;
import io.opentelemetry.contrib.disk.buffering.storage.StorageConfiguration;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.io.File;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SpanDiskExporterTest {
  private SpanExporter wrapped;
  private SpanDiskExporter exporter;
  private static final StorageConfiguration STORAGE_CONFIGURATION =
      TestData.getDefaultConfiguration();
  private static final String STORAGE_FOLDER_NAME = "spans";
  @TempDir File rootDir;

  @BeforeEach
  public void setUp() throws IOException {
    wrapped = mock();
    exporter = new SpanDiskExporter(wrapped, rootDir, STORAGE_CONFIGURATION);
  }

  @Test
  public void verifyCacheFolderName() {
    File[] files = rootDir.listFiles();
    assertEquals(1, files.length);
    assertEquals(STORAGE_FOLDER_NAME, files[0].getName());
  }

  @Test
  public void onFlush_returnSuccess() {
    assertEquals(CompletableResultCode.ofSuccess(), exporter.flush());
  }
}
