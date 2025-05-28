/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.internal.exporter;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.opentelemetry.contrib.disk.buffering.internal.serialization.serializers.SignalSerializer;
import io.opentelemetry.contrib.disk.buffering.internal.storage.Storage;
import io.opentelemetry.sdk.common.CompletableResultCode;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ToDiskExporterTest {

  private final List<String> records = Arrays.asList("one", "two", "three");

  private final byte[] serialized = "one,two,three".getBytes(UTF_8);

  @Mock private SignalSerializer<String> serializer;

  @Mock private Storage storage;
  private ToDiskExporter<String> toDiskExporter;
  private Function<Collection<String>, CompletableResultCode> exportFn;
  private Collection<String> exportedFnSeen;
  private AtomicReference<CompletableResultCode> exportFnResultToReturn;

  @BeforeEach
  void setup() {
    exportedFnSeen = null;
    exportFnResultToReturn = new AtomicReference<>(null);
    exportFn =
        (Collection<String> x) -> {
          exportedFnSeen = x;
          return exportFnResultToReturn.get();
        };
    toDiskExporter = new ToDiskExporter<>(serializer, exportFn, storage);
    when(serializer.serialize(records)).thenReturn(serialized);
  }

  @Test
  void whenWritingSucceedsOnExport_returnSuccessfulResultCode() throws Exception {
    when(storage.write(serialized)).thenReturn(true);
    CompletableResultCode completableResultCode = toDiskExporter.export(records);
    assertThat(completableResultCode.isSuccess()).isTrue();
    verify(storage).write(serialized);
    assertThat(exportedFnSeen).isNull();
  }

  @Test
  void whenWritingFailsOnExport_doExportRightAway() throws Exception {
    when(storage.write(serialized)).thenReturn(false);
    exportFnResultToReturn.set(CompletableResultCode.ofSuccess());

    CompletableResultCode completableResultCode = toDiskExporter.export(records);

    assertThat(completableResultCode.isSuccess()).isTrue();
    assertThat(exportedFnSeen).isEqualTo(records);
  }

  @Test
  void whenExceptionInWrite_doExportRightAway() throws Exception {
    when(storage.write(serialized)).thenThrow(new IOException("boom"));
    exportFnResultToReturn.set(CompletableResultCode.ofFailure());

    CompletableResultCode completableResultCode = toDiskExporter.export(records);

    assertThat(completableResultCode.isSuccess()).isFalse();
    assertThat(exportedFnSeen).isEqualTo(records);
  }

  @Test
  void shutdownClosesStorage() throws Exception {
    toDiskExporter.export(records);
    toDiskExporter.shutdown();
    verify(storage).close();
  }
}
