/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.metrics.micrometer.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MemoizingSupplierTest {

  @Mock Supplier<String> supplier;

  @Test
  void callsGetOnlyOnce() {
    when(supplier.get()).thenReturn("RESULT");

    Supplier<String> underTest = new MemoizingSupplier<>(supplier);

    verifyNoInteractions(supplier);

    assertThat(underTest.get()).isEqualTo("RESULT");
    verify(supplier, times(1)).get();

    assertThat(underTest.get()).isEqualTo("RESULT");
    verifyNoMoreInteractions(supplier);
  }
}
