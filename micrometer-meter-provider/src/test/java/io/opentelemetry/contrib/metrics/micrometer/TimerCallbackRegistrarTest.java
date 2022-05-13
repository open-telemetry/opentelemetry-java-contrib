/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.metrics.micrometer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TimerCallbackRegistrarTest {

  @Mock Timer timer;

  @Mock Runnable callback;

  @Mock TimerTask timerTask;

  @Mock Function<Runnable, TimerTask> timerTaskFunction;

  @Captor ArgumentCaptor<TimerTask> timerTaskArgumentCaptor;

  @Test
  @SuppressWarnings("PreferJavaTimeOverload")
  void createsTimerRegistrarWithConfiguredDelayAndPeriod() {
    try (CallbackRegistrar underTest =
        TimerCallbackRegistrar.builder()
            .setDelay(2, TimeUnit.SECONDS)
            .setPeriod(10, TimeUnit.SECONDS)
            .build()) {

      assertThat(underTest)
          .isInstanceOfSatisfying(
              TimerCallbackRegistrar.class,
              instance -> {
                assertThat(instance.delay).isEqualTo(2000L);
                assertThat(instance.period).isEqualTo(10_000L);
              });
    }
  }

  @Test
  void createsTimerRegistrarWithConfiguredDelayAndPeriodFromDuration() {
    try (CallbackRegistrar underTest =
        TimerCallbackRegistrar.builder()
            .setDelay(Duration.ofSeconds(2))
            .setPeriod(Duration.ofSeconds(10))
            .build()) {

      assertThat(underTest)
          .isInstanceOfSatisfying(
              TimerCallbackRegistrar.class,
              instance -> {
                assertThat(instance.delay).isEqualTo(2000L);
                assertThat(instance.period).isEqualTo(10_000L);
              });
    }
  }

  @Test
  void schedulesCallback() {

    TimerCallbackRegistrar underTest = new TimerCallbackRegistrar(timer, 1000, 1000);
    underTest.registerCallback(callback);

    verify(timer).scheduleAtFixedRate(timerTaskArgumentCaptor.capture(), eq(1000L), eq(1000L));
    TimerTask timerTask = timerTaskArgumentCaptor.getValue();

    timerTask.run();
    verify(callback).run();
  }

  @Test
  void handlesNullCallback() {

    TimerCallbackRegistrar underTest = new TimerCallbackRegistrar(timer, 1000, 1000);
    CallbackRegistration registration = underTest.registerCallback(null);

    verifyNoInteractions(timer);

    assertDoesNotThrow(registration::close);
  }

  @Test
  void closeCancelsTimerTask() {

    when(timerTaskFunction.apply(callback)).thenReturn(timerTask);

    TimerCallbackRegistrar underTest =
        new TimerCallbackRegistrar(timer, 1000, 1000, timerTaskFunction);
    CallbackRegistration registration = underTest.registerCallback(callback);

    verify(timerTaskFunction).apply(callback);
    verify(timer).scheduleAtFixedRate(timerTask, 1000L, 1000L);

    registration.close();
    verify(timerTask).cancel();
  }

  @Test
  void closeCancelsTimer() {
    TimerCallbackRegistrar underTest = new TimerCallbackRegistrar(timer, 1000, 1000);

    underTest.close();
    verify(timer).cancel();
  }
}
