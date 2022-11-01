/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.resourceproviders;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class DelegatingServiceNameDetectorTest {

  @Test
  void detect() throws Exception {
    ServiceNameDetector d1 = mock(ServiceNameDetector.class);
    ServiceNameDetector d2 = mock(ServiceNameDetector.class);
    when(d2.detect()).thenReturn("zzz");
    List<ServiceNameDetector> delegates = Arrays.asList(d1, d2);

    DelegatingServiceNameDetector detector = new DelegatingServiceNameDetector(delegates);
    String result = detector.detect();
    assertThat(result).isEqualTo("zzz");
    verify(d1).detect();
  }

  @Test
  void detectNothing() throws Exception {
    ServiceNameDetector d1 = mock(ServiceNameDetector.class);
    ServiceNameDetector d2 = mock(ServiceNameDetector.class);
    List<ServiceNameDetector> delegates = Arrays.asList(d1, d2);

    DelegatingServiceNameDetector detector = new DelegatingServiceNameDetector(delegates);
    String result = detector.detect();
    assertThat(result).isNull();
    verify(d1).detect();
    verify(d2).detect();
  }

  @Test
  void delegateThrowsButIterationContinues() throws Exception {
    ServiceNameDetector d1 = mock(ServiceNameDetector.class);
    ServiceNameDetector d2 = mock(ServiceNameDetector.class);
    when(d1.detect()).thenThrow(new RuntimeException("kablooey!"));
    when(d2.detect()).thenReturn("bbb");
    List<ServiceNameDetector> delegates = Arrays.asList(d1, d2);

    DelegatingServiceNameDetector detector = new DelegatingServiceNameDetector(delegates);
    String result = detector.detect();
    assertThat(result).isEqualTo("bbb");
    verify(d1).detect();
  }
}
