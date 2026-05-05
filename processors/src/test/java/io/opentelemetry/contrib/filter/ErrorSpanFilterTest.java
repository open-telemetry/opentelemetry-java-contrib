/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.data.StatusData;
import org.junit.jupiter.api.Test;

class ErrorSpanFilterTest {

  private final ErrorSpanFilter filter = new ErrorSpanFilter();

  @Test
  void errorSpanIsKept() {
    SpanData span = spanWithStatus(StatusCode.ERROR);
    assertThat(filter.shouldKeep(span)).isTrue();
  }

  @Test
  void okSpanIsDropped() {
    SpanData span = spanWithStatus(StatusCode.OK);
    assertThat(filter.shouldKeep(span)).isFalse();
  }

  @Test
  void unsetSpanIsDropped() {
    SpanData span = spanWithStatus(StatusCode.UNSET);
    assertThat(filter.shouldKeep(span)).isFalse();
  }

  private static SpanData spanWithStatus(StatusCode statusCode) {
    SpanData span = mock(SpanData.class);
    StatusData status = mock(StatusData.class);
    when(status.getStatusCode()).thenReturn(statusCode);
    when(span.getStatus()).thenReturn(status);
    return span;
  }
}
