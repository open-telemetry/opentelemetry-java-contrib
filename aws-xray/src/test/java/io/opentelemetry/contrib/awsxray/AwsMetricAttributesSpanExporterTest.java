/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.awsxray;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.sdk.common.InstrumentationScopeInfo;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.data.EventData;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.data.StatusData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;

/** Unit tests for {@link AwsSpanMetricsProcessor}. */
class AwsMetricAttributesSpanExporterTest {

  @Captor private static ArgumentCaptor<Collection<SpanData>> delegateExportCaptor;

  // Test constants
  private static final boolean CONTAINS_ATTRIBUTES = true;
  private static final boolean CONTAINS_NO_ATTRIBUTES = false;

  // Resource is not mockable, but tests can safely rely on an empty resource.
  private static final Resource testResource = Resource.empty();

  // Mocks required for tests.
  private MetricAttributeGenerator generatorMock;
  private SpanExporter delegateMock;

  private AwsMetricAttributesSpanExporter awsMetricAttributesSpanExporter;

  @BeforeEach
  public void setUpMocks() {
    MockitoAnnotations.openMocks(this);
    generatorMock = mock(MetricAttributeGenerator.class);
    delegateMock = mock(SpanExporter.class);

    awsMetricAttributesSpanExporter =
        AwsMetricAttributesSpanExporter.create(delegateMock, generatorMock, testResource);
  }

  @Test
  public void testPassthroughDelegations() {
    awsMetricAttributesSpanExporter.flush();
    awsMetricAttributesSpanExporter.shutdown();
    awsMetricAttributesSpanExporter.close();
    verify(delegateMock, times(1)).flush();
    verify(delegateMock, times(1)).shutdown();
    verify(delegateMock, times(1)).close();
  }

  @Test
  public void testExportDelegationWithoutAttributeOrModification() {
    Attributes spanAttributes = buildSpanAttributes(CONTAINS_NO_ATTRIBUTES);
    SpanData spanDataMock = buildSpanDataMock(spanAttributes);
    Attributes metricAttributes = buildMetricAttributes(CONTAINS_NO_ATTRIBUTES);
    configureMocksForExport(spanDataMock, metricAttributes);

    awsMetricAttributesSpanExporter.export(Collections.singletonList(spanDataMock));
    verify(delegateMock, times(1)).export(delegateExportCaptor.capture());
    Collection<SpanData> exportedSpans = delegateExportCaptor.getValue();
    assertThat(exportedSpans.size()).isEqualTo(1);

    SpanData exportedSpan = (SpanData) exportedSpans.toArray()[0];
    assertThat(exportedSpan).isEqualTo(spanDataMock);
  }

  @Test
  public void testExportDelegationWithAttributeButWithoutModification() {
    Attributes spanAttributes = buildSpanAttributes(CONTAINS_ATTRIBUTES);
    SpanData spanDataMock = buildSpanDataMock(spanAttributes);
    Attributes metricAttributes = buildMetricAttributes(CONTAINS_NO_ATTRIBUTES);
    configureMocksForExport(spanDataMock, metricAttributes);

    awsMetricAttributesSpanExporter.export(Collections.singletonList(spanDataMock));
    verify(delegateMock, times(1)).export(delegateExportCaptor.capture());
    Collection<SpanData> exportedSpans = delegateExportCaptor.getValue();
    assertThat(exportedSpans.size()).isEqualTo(1);

    SpanData exportedSpan = (SpanData) exportedSpans.toArray()[0];
    assertThat(exportedSpan).isEqualTo(spanDataMock);
  }

  @Test
  public void testExportDelegationWithoutAttributeButWithModification() {
    Attributes spanAttributes = buildSpanAttributes(CONTAINS_NO_ATTRIBUTES);
    SpanData spanDataMock = buildSpanDataMock(spanAttributes);
    Attributes metricAttributes = buildMetricAttributes(CONTAINS_ATTRIBUTES);
    configureMocksForExport(spanDataMock, metricAttributes);

    awsMetricAttributesSpanExporter.export(Collections.singletonList(spanDataMock));
    verify(delegateMock, times(1)).export(delegateExportCaptor.capture());
    List<SpanData> exportedSpans = (List<SpanData>) delegateExportCaptor.getValue();
    assertThat(exportedSpans.size()).isEqualTo(1);

    SpanData exportedSpan = exportedSpans.get(0);
    assertThat(exportedSpan.getClass()).isNotEqualTo(spanDataMock.getClass());
    assertThat(exportedSpan.getTotalAttributeCount()).isEqualTo(metricAttributes.size());
    Attributes exportedAttributes = exportedSpan.getAttributes();
    assertThat(exportedAttributes.size()).isEqualTo(metricAttributes.size());
    metricAttributes.forEach((k, v) -> assertThat(exportedAttributes.get(k)).isEqualTo(v));
  }

  @Test
  public void testExportDelegationWithAttributeAndModification() {
    Attributes spanAttributes = buildSpanAttributes(CONTAINS_ATTRIBUTES);
    SpanData spanDataMock = buildSpanDataMock(spanAttributes);
    Attributes metricAttributes = buildMetricAttributes(CONTAINS_ATTRIBUTES);
    configureMocksForExport(spanDataMock, metricAttributes);

    awsMetricAttributesSpanExporter.export(Collections.singletonList(spanDataMock));
    verify(delegateMock, times(1)).export(delegateExportCaptor.capture());
    List<SpanData> exportedSpans = (List<SpanData>) delegateExportCaptor.getValue();
    assertThat(exportedSpans.size()).isEqualTo(1);

    SpanData exportedSpan = exportedSpans.get(0);
    assertThat(exportedSpan.getClass()).isNotEqualTo(spanDataMock.getClass());
    int expectedAttributeCount = metricAttributes.size() + spanAttributes.size();
    assertThat(exportedSpan.getTotalAttributeCount()).isEqualTo(expectedAttributeCount);
    Attributes exportedAttributes = exportedSpan.getAttributes();
    assertThat(exportedAttributes.size()).isEqualTo(expectedAttributeCount);
    spanAttributes.forEach((k, v) -> assertThat(exportedAttributes.get(k)).isEqualTo(v));
    metricAttributes.forEach((k, v) -> assertThat(exportedAttributes.get(k)).isEqualTo(v));
  }

  @Test
  public void testExportDelegationWithMultipleSpans() {
    Attributes spanAttributes1 = buildSpanAttributes(CONTAINS_NO_ATTRIBUTES);
    SpanData spanDataMock1 = buildSpanDataMock(spanAttributes1);
    Attributes metricAttributes1 = buildMetricAttributes(CONTAINS_NO_ATTRIBUTES);
    configureMocksForExport(spanDataMock1, metricAttributes1);

    Attributes spanAttributes2 = buildSpanAttributes(CONTAINS_ATTRIBUTES);
    SpanData spanDataMock2 = buildSpanDataMock(spanAttributes2);
    Attributes metricAttributes2 = buildMetricAttributes(CONTAINS_ATTRIBUTES);
    configureMocksForExport(spanDataMock2, metricAttributes2);

    Attributes spanAttributes3 = buildSpanAttributes(CONTAINS_ATTRIBUTES);
    SpanData spanDataMock3 = buildSpanDataMock(spanAttributes3);
    Attributes metricAttributes3 = buildMetricAttributes(CONTAINS_NO_ATTRIBUTES);
    configureMocksForExport(spanDataMock3, metricAttributes3);

    awsMetricAttributesSpanExporter.export(
        Arrays.asList(spanDataMock1, spanDataMock2, spanDataMock3));
    verify(delegateMock, times(1)).export(delegateExportCaptor.capture());
    List<SpanData> exportedSpans = (List<SpanData>) delegateExportCaptor.getValue();
    assertThat(exportedSpans.size()).isEqualTo(3);

    SpanData exportedSpan1 = exportedSpans.get(0);
    SpanData exportedSpan2 = exportedSpans.get(1);
    SpanData exportedSpan3 = exportedSpans.get(2);

    assertThat(exportedSpan1).isEqualTo(spanDataMock1);
    assertThat(exportedSpan3).isEqualTo(spanDataMock3);

    assertThat(exportedSpan2.getClass()).isNotEqualTo(spanDataMock2.getClass());
    int expectedAttributeCount = metricAttributes2.size() + spanAttributes2.size();
    assertThat(exportedSpan2.getTotalAttributeCount()).isEqualTo(expectedAttributeCount);
    Attributes exportedAttributes = exportedSpan2.getAttributes();
    assertThat(exportedAttributes.size()).isEqualTo(expectedAttributeCount);
    spanAttributes2.forEach((k, v) -> assertThat(exportedAttributes.get(k)).isEqualTo(v));
    metricAttributes2.forEach((k, v) -> assertThat(exportedAttributes.get(k)).isEqualTo(v));
  }

  @Test
  public void testOverridenAttributes() {
    Attributes spanAttributes =
        Attributes.of(
            AttributeKey.stringKey("key1"),
            "old value1",
            AttributeKey.stringKey("key2"),
            "old value2");
    SpanData spanDataMock = buildSpanDataMock(spanAttributes);
    Attributes metricAttributes =
        Attributes.of(
            AttributeKey.stringKey("key1"),
            "new value1",
            AttributeKey.stringKey("key3"),
            "new value3");
    configureMocksForExport(spanDataMock, metricAttributes);

    awsMetricAttributesSpanExporter.export(Collections.singletonList(spanDataMock));
    verify(delegateMock, times(1)).export(delegateExportCaptor.capture());
    List<SpanData> exportedSpans = (List<SpanData>) delegateExportCaptor.getValue();
    assertThat(exportedSpans.size()).isEqualTo(1);

    SpanData exportedSpan = exportedSpans.get(0);
    assertThat(exportedSpan.getClass()).isNotEqualTo(spanDataMock.getClass());
    assertThat(exportedSpan.getTotalAttributeCount()).isEqualTo(3);
    Attributes exportedAttributes = exportedSpan.getAttributes();
    assertThat(exportedAttributes.size()).isEqualTo(3);
    assertThat(exportedAttributes.get(AttributeKey.stringKey("key1"))).isEqualTo("new value1");
    assertThat(exportedAttributes.get(AttributeKey.stringKey("key2"))).isEqualTo("old value2");
    assertThat(exportedAttributes.get(AttributeKey.stringKey("key3"))).isEqualTo("new value3");
  }

  @Test
  public void testExportDelegatingSpanDataBehaviour() {
    Attributes spanAttributes = buildSpanAttributes(CONTAINS_ATTRIBUTES);
    SpanData spanDataMock = buildSpanDataMock(spanAttributes);
    Attributes metricAttributes = buildMetricAttributes(CONTAINS_ATTRIBUTES);
    configureMocksForExport(spanDataMock, metricAttributes);

    awsMetricAttributesSpanExporter.export(Collections.singletonList(spanDataMock));
    verify(delegateMock, times(1)).export(delegateExportCaptor.capture());
    List<SpanData> exportedSpans = (List<SpanData>) delegateExportCaptor.getValue();
    assertThat(exportedSpans.size()).isEqualTo(1);

    SpanData exportedSpan = exportedSpans.get(0);

    SpanContext spanContextMock = mock(SpanContext.class);
    when(spanDataMock.getSpanContext()).thenReturn(spanContextMock);
    assertThat(exportedSpan.getSpanContext()).isEqualTo(spanContextMock);

    SpanContext parentSpanContextMock = mock(SpanContext.class);
    when(spanDataMock.getParentSpanContext()).thenReturn(parentSpanContextMock);
    assertThat(exportedSpan.getParentSpanContext()).isEqualTo(parentSpanContextMock);

    when(spanDataMock.getResource()).thenReturn(testResource);
    assertThat(exportedSpan.getResource()).isEqualTo(testResource);

    // InstrumentationLibraryInfo is deprecated, so actually invoking it causes build failures.
    // Excluding from this test.

    InstrumentationScopeInfo testInstrumentationScopeInfo = InstrumentationScopeInfo.empty();
    when(spanDataMock.getInstrumentationScopeInfo()).thenReturn(testInstrumentationScopeInfo);
    assertThat(exportedSpan.getInstrumentationScopeInfo()).isEqualTo(testInstrumentationScopeInfo);

    String testName = "name";
    when(spanDataMock.getName()).thenReturn(testName);
    assertThat(exportedSpan.getName()).isEqualTo(testName);

    SpanKind kindMock = mock(SpanKind.class);
    when(spanDataMock.getKind()).thenReturn(kindMock);
    assertThat(exportedSpan.getKind()).isEqualTo(kindMock);

    long testStartEpochNanos = 1L;
    when(spanDataMock.getStartEpochNanos()).thenReturn(testStartEpochNanos);
    assertThat(exportedSpan.getStartEpochNanos()).isEqualTo(testStartEpochNanos);

    List<EventData> eventsMock = Collections.singletonList(mock(EventData.class));
    when(spanDataMock.getEvents()).thenReturn(eventsMock);
    assertThat(exportedSpan.getEvents()).isEqualTo(eventsMock);

    List<LinkData> linksMock = Collections.singletonList(mock(LinkData.class));
    when(spanDataMock.getLinks()).thenReturn(linksMock);
    assertThat(exportedSpan.getLinks()).isEqualTo(linksMock);

    StatusData statusMock = mock(StatusData.class);
    when(spanDataMock.getStatus()).thenReturn(statusMock);
    assertThat(exportedSpan.getStatus()).isEqualTo(statusMock);

    long testEndEpochNanosMock = 2L;
    when(spanDataMock.getEndEpochNanos()).thenReturn(testEndEpochNanosMock);
    assertThat(exportedSpan.getEndEpochNanos()).isEqualTo(testEndEpochNanosMock);

    when(spanDataMock.hasEnded()).thenReturn(true);
    assertThat(exportedSpan.hasEnded()).isEqualTo(true);

    int testTotalRecordedEventsMock = 3;
    when(spanDataMock.getTotalRecordedEvents()).thenReturn(testTotalRecordedEventsMock);
    assertThat(exportedSpan.getTotalRecordedEvents()).isEqualTo(testTotalRecordedEventsMock);

    int testTotalRecordedLinksMock = 4;
    when(spanDataMock.getTotalRecordedLinks()).thenReturn(testTotalRecordedLinksMock);
    assertThat(exportedSpan.getTotalRecordedLinks()).isEqualTo(testTotalRecordedLinksMock);
  }

  private static Attributes buildSpanAttributes(boolean containsAttribute) {
    if (containsAttribute) {
      return Attributes.of(AttributeKey.stringKey("original key"), "original value");
    } else {
      return Attributes.empty();
    }
  }

  private static Attributes buildMetricAttributes(boolean containsAttribute) {
    if (containsAttribute) {
      return Attributes.of(AttributeKey.stringKey("new key"), "new value");
    } else {
      return Attributes.empty();
    }
  }

  private static SpanData buildSpanDataMock(Attributes spanAttributes) {
    // Configure spanData
    SpanData mockSpanData = mock(SpanData.class);
    when(mockSpanData.getAttributes()).thenReturn(spanAttributes);
    when(mockSpanData.getTotalAttributeCount()).thenReturn(spanAttributes.size());
    return mockSpanData;
  }

  private void configureMocksForExport(SpanData spanDataMock, Attributes metricAttributes) {
    // Configure generated attributes
    when(generatorMock.generateMetricAttributesFromSpan(eq(spanDataMock), eq(testResource)))
        .thenReturn(metricAttributes);
  }
}
