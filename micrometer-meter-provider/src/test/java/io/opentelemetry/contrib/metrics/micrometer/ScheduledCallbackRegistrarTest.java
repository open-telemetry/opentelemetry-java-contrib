/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.metrics.micrometer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ScheduledCallbackRegistrarTest {

  @Mock ScheduledExecutorService scheduledExecutorService;

  @Mock Runnable callback;

  @Mock ScheduledFuture<?> scheduledFuture;

  @Test
  void schedulesCallback() {
    doReturn(scheduledFuture)
        .when(scheduledExecutorService)
        .scheduleAtFixedRate(any(), anyLong(), anyLong(), any());

    try (CallbackRegistrar underTest =
        ScheduledCallbackRegistrar.builder(scheduledExecutorService).build()) {

      underTest.registerCallback(callback);

      verify(scheduledExecutorService).scheduleAtFixedRate(callback, 1L, 1L, TimeUnit.SECONDS);
    }
  }

  @Test
  @SuppressWarnings("PreferJavaTimeOverload")
  void schedulesCallbackWithPeriod() {
    doReturn(scheduledFuture)
        .when(scheduledExecutorService)
        .scheduleAtFixedRate(any(), anyLong(), anyLong(), any());

    try (CallbackRegistrar underTest =
        ScheduledCallbackRegistrar.builder(scheduledExecutorService)
            .setPeriod(10L, TimeUnit.SECONDS)
            .build()) {

      underTest.registerCallback(callback);

      verify(scheduledExecutorService).scheduleAtFixedRate(callback, 10L, 10L, TimeUnit.SECONDS);
    }
  }

  @Test
  void schedulesCallbackWithPeriodDuration() {
    doReturn(scheduledFuture)
        .when(scheduledExecutorService)
        .scheduleAtFixedRate(any(), anyLong(), anyLong(), any());

    try (CallbackRegistrar underTest =
        ScheduledCallbackRegistrar.builder(scheduledExecutorService)
            .setPeriod(Duration.ofSeconds(10L))
            .build()) {

      underTest.registerCallback(callback);

      verify(scheduledExecutorService)
          .scheduleAtFixedRate(callback, 10_000L, 10_000L, TimeUnit.MILLISECONDS);
    }
  }

  @Test
  void handlesNullCallback() {
    try (CallbackRegistrar underTest =
        ScheduledCallbackRegistrar.builder(scheduledExecutorService).build()) {

      underTest.registerCallback(null);

      verifyNoInteractions(scheduledExecutorService);
    }
  }

  @Test
  void closeCancelsScheduledFuture() {
    doReturn(scheduledFuture)
        .when(scheduledExecutorService)
        .scheduleAtFixedRate(any(), anyLong(), anyLong(), any());

    try (CallbackRegistrar underTest =
        ScheduledCallbackRegistrar.builder(scheduledExecutorService).build()) {

      CallbackRegistration callbackRegistration = underTest.registerCallback(callback);
      verify(scheduledExecutorService).scheduleAtFixedRate(callback, 1L, 1L, TimeUnit.SECONDS);

      callbackRegistration.close();
      verify(scheduledFuture).cancel(false);
    }
  }

  @Test
  void closeShutsDownScheduledExecutorService() {
    CallbackRegistrar underTest =
        ScheduledCallbackRegistrar.builder(scheduledExecutorService).build();

    underTest.close();
    verify(scheduledExecutorService).shutdown();
  }
}
