package io.opentelemetry.contrib.jfr.metrics;

import org.junit.jupiter.api.Test;
import org.moditect.jfrunit.*;

import java.time.Duration;

public class JfrCPUTest {

  @Test
  public void shouldHaveGcAndSleepEvents() throws Exception {
    System.gc();
    Thread.sleep(1000);

    jfrEvents.awaitEvents();

    assertThat(jfrEvents).contains(event("jdk.GarbageCollection"));
    assertThat(jfrEvents).contains(
        event("jdk.ThreadSleep").with("time", Duration.ofSeconds(1)));
  }

}
