/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.metrics.micrometer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ScheduledCallbackRegistrarTest {

  @Mock ScheduledExecutorService scheduledExecutorService;

  @Mock Runnable callback;

  @Mock ScheduledFuture<?> scheduledFuture;

  @Captor ArgumentCaptor<Runnable> pollingRunnableCaptor;

  @Test
  void schedulesCallback() {
    doReturn(scheduledFuture)
        .when(scheduledExecutorService)
        .scheduleAtFixedRate(any(), anyLong(), anyLong(), any());

    try (CallbackRegistrar underTest =
        ScheduledCallbackRegistrar.builder(scheduledExecutorService).build()) {

      verifyNoInteractions(scheduledExecutorService);

      underTest.registerCallback(callback);

      verify(scheduledExecutorService)
          .scheduleAtFixedRate(
              pollingRunnableCaptor.capture(), eq(1L), eq(1L), eq(TimeUnit.SECONDS));
      Runnable pollingRunnable = pollingRunnableCaptor.getValue();

      verifyNoInteractions(callback);

      pollingRunnable.run();
      verify(callback).run();
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

      verifyNoInteractions(scheduledExecutorService);

      underTest.registerCallback(callback);

      verify(scheduledExecutorService)
          .scheduleAtFixedRate(
              pollingRunnableCaptor.capture(), eq(10L), eq(10L), eq(TimeUnit.SECONDS));
      Runnable pollingRunnable = pollingRunnableCaptor.getValue();

      verifyNoInteractions(callback);

      pollingRunnable.run();
      verify(callback).run();
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

      verifyNoInteractions(scheduledExecutorService);

      underTest.registerCallback(callback);

      verify(scheduledExecutorService)
          .scheduleAtFixedRate(
              pollingRunnableCaptor.capture(), eq(10_000L), eq(10_000L), eq(TimeUnit.MILLISECONDS));
      Runnable pollingRunnable = pollingRunnableCaptor.getValue();

      verifyNoInteractions(callback);

      pollingRunnable.run();
      verify(callback).run();
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

      underTest.registerCallback(callback);
      verify(scheduledExecutorService)
          .scheduleAtFixedRate(any(), eq(1L), eq(1L), eq(TimeUnit.SECONDS));
    }
    verify(scheduledFuture).cancel(false);
  }

  @Test
  void closeShutsDownScheduledExecutorService() {
    CallbackRegistrar underTest =
        ScheduledCallbackRegistrar.builder(scheduledExecutorService)
            .setShutdownExecutorOnClose(true)
            .build();

    underTest.close();
    verify(scheduledExecutorService).shutdown();
  }

  @Test
  void closeDoesNotShutDownScheduledExecutorService() {
    CallbackRegistrar underTest =
        ScheduledCallbackRegistrar.builder(scheduledExecutorService)
            .setShutdownExecutorOnClose(false)
            .build();

    underTest.close();
    verify(scheduledExecutorService, never()).shutdown();
  }
}
