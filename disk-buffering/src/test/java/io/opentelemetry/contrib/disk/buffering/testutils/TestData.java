/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.disk.buffering.testutils;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.sdk.common.InstrumentationScopeInfo;
import io.opentelemetry.sdk.metrics.data.GaugeData;
import io.opentelemetry.sdk.metrics.data.LongExemplarData;
import io.opentelemetry.sdk.metrics.data.LongPointData;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.internal.data.ImmutableGaugeData;
import io.opentelemetry.sdk.metrics.internal.data.ImmutableLongExemplarData;
import io.opentelemetry.sdk.metrics.internal.data.ImmutableLongPointData;
import io.opentelemetry.sdk.metrics.internal.data.ImmutableMetricData;
import io.opentelemetry.sdk.resources.Resource;
import java.util.Collections;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unchecked")
public final class TestData {
  public static final String TRACE_ID = "b535b3b5232b5dabced5b0ab8037eb78";
  public static final String SPAN_ID = "f3fc364fb6b77cff";
  public static final String PARENT_SPAN_ID = "d3fc364fb6b77cfa";
  public static final Attributes ATTRIBUTES =
      Attributes.builder()
          .put("bear", "mya")
          .put("warm", true)
          .put("temperature", 30)
          .put("length", 1.2)
          .put("colors", "red", "blue")
          .put("conditions", false, true)
          .put("scores", 0L, 1L)
          .put("coins", 0.01, 0.05, 0.1)
          .put("empty", "")
          .put("blank", " ")
          .build();

  public static final Resource RESOURCE_FULL =
      Resource.create(
          Attributes.builder().put("resourceAttr", "resourceAttrValue").build(),
          "resourceSchemaUrl");

  public static final Resource RESOURCE_WITHOUT_SCHEMA_URL =
      Resource.create(Attributes.builder().put("resourceAttr", "resourceAttrValue").build());

  public static final SpanContext SPAN_CONTEXT =
      SpanContext.create(TRACE_ID, SPAN_ID, TraceFlags.getDefault(), TraceState.getDefault());
  public static final SpanContext SPAN_CONTEXT_WITH_TRACE_STATE =
      SpanContext.create(
          TRACE_ID,
          SPAN_ID,
          TraceFlags.getSampled(),
          TraceState.builder().put("aaa", "bbb").put("ccc", "ddd").build());
  public static final SpanContext PARENT_SPAN_CONTEXT =
      SpanContext.create(
          TRACE_ID, PARENT_SPAN_ID, TraceFlags.getSampled(), TraceState.getDefault());
  public static final InstrumentationScopeInfo INSTRUMENTATION_SCOPE_INFO_FULL =
      InstrumentationScopeInfo.builder("Instrumentation scope name")
          .setVersion("1.2.3")
          .setSchemaUrl("instrumentationScopeInfoSchemaUrl")
          .setAttributes(
              Attributes.builder()
                  .put("instrumentationScopeInfoAttr", "instrumentationScopeInfoAttrValue")
                  .build())
          .build();

  public static final InstrumentationScopeInfo INSTRUMENTATION_SCOPE_INFO_WITHOUT_VERSION =
      InstrumentationScopeInfo.builder("Instrumentation scope name")
          .setSchemaUrl("instrumentationScopeInfoSchemaUrl")
          .setAttributes(
              Attributes.builder()
                  .put("instrumentationScopeInfoAttr", "instrumentationScopeInfoAttrValue")
                  .build())
          .build();

  @NotNull
  public static MetricData makeLongGauge(TraceFlags flags) {
    return makeLongGauge(flags, RESOURCE_FULL, INSTRUMENTATION_SCOPE_INFO_FULL);
  }

  @NotNull
  public static MetricData makeLongGauge(
      TraceFlags flags, InstrumentationScopeInfo instrumentationScopeInfo) {
    return makeLongGauge(flags, RESOURCE_FULL, instrumentationScopeInfo);
  }

  @NotNull
  public static MetricData makeLongGauge(TraceFlags flags, Resource resource) {
    return makeLongGauge(flags, resource, INSTRUMENTATION_SCOPE_INFO_FULL);
  }

  @NotNull
  public static MetricData makeLongGauge(
      TraceFlags flags, Resource resource, InstrumentationScopeInfo instrumentationScopeInfo) {
    LongPointData point = makeLongPointData(flags);
    GaugeData<LongPointData> gaugeData =
        ImmutableGaugeData.create(Collections.singletonList(point));
    return ImmutableMetricData.createLongGauge(
        resource,
        instrumentationScopeInfo,
        "Long gauge name",
        "Long gauge description",
        "ms",
        gaugeData);
  }

  @NotNull
  public static LongPointData makeLongPointData(TraceFlags flags) {
    LongExemplarData longExemplarData = makeLongExemplarData(flags);
    return ImmutableLongPointData.create(
        1L, 2L, ATTRIBUTES, 1L, Collections.singletonList(longExemplarData));
  }

  @NotNull
  public static SpanContext makeContext(TraceFlags flags) {
    return makeContext(flags, SPAN_ID);
  }

  @NotNull
  public static SpanContext makeContext(TraceFlags flags, String spanId) {
    return SpanContext.create(TRACE_ID, spanId, flags, TraceState.getDefault());
  }

  @NotNull
  private static LongExemplarData makeLongExemplarData(TraceFlags flags) {
    SpanContext context = makeContext(flags);
    return ImmutableLongExemplarData.create(ATTRIBUTES, 100L, context, 1L);
  }

  @NotNull
  public static byte[] makeTooShortSignalBinary() {
    return new byte[] {
      (byte) 0x0A, // type
      (byte) 0xFF, // defining length 255, but message is shorter
      (byte) 0x01 // content
    };
  }

  @NotNull
  public static byte[] makeMalformedSignalBinary() {
    return new byte[] {
      (byte) 0x0A, // type
      (byte) 0x02, // length
      (byte) 0x08, // field 1, wire type 0 (varint) - this should be a nested message but isn't
      (byte) 0x01 // content
    };
  }

  private TestData() {}
}
