package io.opentelemetry.contrib.jfr.metrics;

import org.junit.jupiter.api.Test;

class JfrCPUTest extends AbstractMetricsTest {

  @Test
  public void shouldHaveGcAndLockEvents() throws Exception {
    System.gc();
    synchronized (this) {
      Thread.sleep(1000);
    }
  }

}
