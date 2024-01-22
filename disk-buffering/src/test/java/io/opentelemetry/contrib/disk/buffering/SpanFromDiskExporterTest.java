/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.contrib.disk.buffering.internal.StorageConfiguration;
import io.opentelemetry.contrib.disk.buffering.internal.files.DefaultTemporaryFileProvider;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.serializers.SignalSerializer;
import io.opentelemetry.contrib.disk.buffering.internal.storage.Storage;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.testing.trace.TestSpanData;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.data.StatusData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

class SpanFromDiskExporterTest {

  @TempDir File tempDir;

  @SuppressWarnings("unchecked")
  @Test
  void fromDisk() throws Exception {
    StorageConfiguration config =
        StorageConfiguration.builder()
            .setRootDir(tempDir)
            .setMaxFileAgeForWriteMillis(TimeUnit.HOURS.toMillis(24))
            .setMinFileAgeForReadMillis(0)
            .setMaxFileAgeForReadMillis(TimeUnit.HOURS.toMillis(24))
            .setTemporaryFileProvider(DefaultTemporaryFileProvider.getInstance())
            .build();

    List<SpanData> spans = writeSomeSpans(config);

    SpanExporter exporter = mock();
    ArgumentCaptor<Collection<SpanData>> capture = ArgumentCaptor.forClass(Collection.class);
    when(exporter.export(capture.capture())).thenReturn(CompletableResultCode.ofSuccess());

    SpanFromDiskExporter testClass = SpanFromDiskExporter.create(exporter, config);
    boolean result = testClass.exportStoredBatch(30, TimeUnit.SECONDS);
    assertThat(result).isTrue();
    List<SpanData> exportedSpans = (List<SpanData>) capture.getValue();
    assertThat(exportedSpans.get(1)).isEqualTo(spans.get(1));
    assertThat(exportedSpans.get(0)).isEqualTo(spans.get(0));
    verify(exporter).export(eq(spans));
  }

  private static List<SpanData> writeSomeSpans(StorageConfiguration config) throws Exception {
    long now = System.currentTimeMillis() * 1_000_000;
    TestSpanData span1 =
        TestSpanData.builder()
            .setName("span1")
            .setSpanContext(
                SpanContext.create(
                    "abc123", "fff1", TraceFlags.getDefault(), TraceState.getDefault()))
            .setStatus(StatusData.create(StatusCode.OK, "whatever"))
            .setHasEnded(true)
            .setAttributes(Attributes.of(AttributeKey.stringKey("foo"), "bar"))
            .setKind(SpanKind.SERVER)
            .setStartEpochNanos(now)
            .setEndEpochNanos(now + 50_000_000)
            .build();
    TestSpanData span2 =
        TestSpanData.builder()
            .setName("span2")
            .setSpanContext(
                SpanContext.create(
                    "abc123", "fff2", TraceFlags.getSampled(), TraceState.getDefault()))
            .setStatus(StatusData.create(StatusCode.OK, "excellent"))
            .setHasEnded(true)
            .setAttributes(Attributes.of(AttributeKey.stringKey("bar"), "baz"))
            .setKind(SpanKind.CLIENT)
            .setStartEpochNanos(now + 12)
            .setEndEpochNanos(now + 12 + 40_000_000)
            .build();
    List<SpanData> spans = Arrays.asList(span1, span2);

    SignalSerializer<SpanData> serializer = SignalSerializer.ofSpans();
    File subdir = new File(config.getRootDir(), "spans");
    assertTrue(subdir.mkdir());

    Storage storage =
        Storage.builder().setStorageConfiguration(config).setFolderName("spans").build();
    storage.write(serializer.serialize(spans));
    storage.close();
    return spans;
  }
}
