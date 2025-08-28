/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering;

import static org.assertj.core.api.Assertions.assertThat;
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
import io.opentelemetry.contrib.disk.buffering.config.StorageConfiguration;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.spans.models.SpanDataImpl;
import io.opentelemetry.contrib.disk.buffering.internal.serialization.serializers.SignalSerializer;
import io.opentelemetry.contrib.disk.buffering.internal.storage.Storage;
import io.opentelemetry.contrib.disk.buffering.internal.utils.SignalTypes;
import io.opentelemetry.contrib.disk.buffering.testutils.TestData;
import io.opentelemetry.sdk.common.Clock;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.data.StatusData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
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
    Clock clock = mock(Clock.class);
    long start = TimeUnit.MILLISECONDS.toNanos(System.currentTimeMillis());
    when(clock.now()).thenReturn(start);
    Storage storage =
        Storage.builder(SignalTypes.spans)
            .setStorageConfiguration(StorageConfiguration.builder().setRootDir(tempDir).build())
            .setStorageClock(clock)
            .build();

    List<SpanData> spans = writeSomeSpans(storage);

    when(clock.now()).thenReturn(start + TimeUnit.SECONDS.toNanos(60));

    SpanExporter exporter = mock();
    ArgumentCaptor<Collection<SpanData>> capture = ArgumentCaptor.forClass(Collection.class);
    when(exporter.export(capture.capture())).thenReturn(CompletableResultCode.ofSuccess());

    SpanFromDiskExporter testClass = SpanFromDiskExporter.create(exporter, storage);
    boolean result = testClass.exportStoredBatch(30, TimeUnit.SECONDS);
    assertThat(result).isTrue();
    List<SpanData> exportedSpans = (List<SpanData>) capture.getValue();

    long now = spans.get(0).getStartEpochNanos();
    SpanData expected1 = makeSpan1(TraceFlags.getSampled(), now);
    SpanData expected2 = makeSpan2(TraceFlags.getSampled(), now);

    assertThat(exportedSpans.get(0)).isEqualTo(expected1);
    assertThat(exportedSpans.get(1)).isEqualTo(expected2);
    assertThat(exportedSpans).containsExactly(expected1, expected2);

    verify(exporter).export(eq(Arrays.asList(expected1, expected2)));
  }

  private static List<SpanData> writeSomeSpans(Storage storage) throws Exception {
    long now = System.currentTimeMillis() * 1_000_000;
    SpanData span1 = makeSpan1(TraceFlags.getDefault(), now);
    SpanData span2 = makeSpan2(TraceFlags.getSampled(), now);
    List<SpanData> spans = Arrays.asList(span1, span2);

    storage.write(SignalSerializer.ofSpans().initialize(spans));
    storage.flush();
    return spans;
  }

  private static SpanData makeSpan1(TraceFlags parentSpanContextFlags, long now) {
    Attributes attributes = Attributes.of(AttributeKey.stringKey("foo"), "bar");
    SpanContext parentContext = TestData.makeContext(parentSpanContextFlags, TestData.SPAN_ID);
    return SpanDataImpl.builder()
        .setName("span1")
        .setSpanContext(
            SpanContext.create(
                TestData.TRACE_ID,
                TestData.SPAN_ID,
                TraceFlags.getDefault(),
                TraceState.getDefault()))
        .setParentSpanContext(parentContext)
        .setInstrumentationScopeInfo(TestData.INSTRUMENTATION_SCOPE_INFO_FULL)
        .setStatus(StatusData.create(StatusCode.OK, "whatever"))
        .setAttributes(attributes)
        .setKind(SpanKind.SERVER)
        .setStartEpochNanos(now)
        .setEndEpochNanos(now + 50_000_000)
        .setTotalRecordedEvents(0)
        .setTotalRecordedLinks(0)
        .setTotalAttributeCount(attributes.size())
        .setLinks(Collections.emptyList())
        .setEvents(Collections.emptyList())
        .setResource(Resource.getDefault())
        .build();
  }

  private static SpanData makeSpan2(TraceFlags parentSpanContextFlags, long now) {
    Attributes attributes = Attributes.of(AttributeKey.stringKey("bar"), "baz");
    String spanId = "aaaaaaaaa12312312";
    SpanContext parentContext = TestData.makeContext(parentSpanContextFlags, spanId);
    return SpanDataImpl.builder()
        .setName("span2")
        .setSpanContext(
            SpanContext.create(
                TestData.TRACE_ID, spanId, TraceFlags.getSampled(), TraceState.getDefault()))
        .setParentSpanContext(parentContext)
        .setInstrumentationScopeInfo(TestData.INSTRUMENTATION_SCOPE_INFO_FULL)
        .setStatus(StatusData.create(StatusCode.OK, "excellent"))
        .setAttributes(attributes)
        .setKind(SpanKind.CLIENT)
        .setStartEpochNanos(now + 12)
        .setEndEpochNanos(now + 12 + 40_000_000)
        .setTotalRecordedEvents(0)
        .setTotalRecordedLinks(0)
        .setTotalAttributeCount(attributes.size())
        .setLinks(Collections.emptyList())
        .setEvents(Collections.emptyList())
        .setResource(Resource.getDefault())
        .build();
  }
}
