package io.opentelemetry.contrib.disk.buffering.testutils;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import io.opentelemetry.contrib.disk.buffering.internal.storage.utils.TimeProvider;
import java.lang.reflect.Field;

public class FakeTimeProvider {

  private FakeTimeProvider() {
  }

  public static TimeProvider createAndSetMock(long initialTimeInMillis) {
    TimeProvider timeProvider = mock();
    try {
      Field field = TimeProvider.class.getDeclaredField("instance");
      field.setAccessible(true);
      field.set(null, timeProvider);
      doReturn(initialTimeInMillis).when(timeProvider).getSystemCurrentTimeMillis();
      return timeProvider;
    } catch (NoSuchFieldException | IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }
}
