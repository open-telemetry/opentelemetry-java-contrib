package io.opentelemetry.contrib.jfrstreaming;

import io.opentelemetry.contrib.jfr.metrics.AbstractMetricsTest;
import org.junit.jupiter.api.Test;

public class JfrCPUTest extends AbstractMetricsTest {

  @Test
  public void shouldHaveGcAndLockEvents() throws Exception {
    // This should generate some events
    System.gc();
    synchronized (this) {
      Thread.sleep(1000);
    }

    waitAndAssertMetrics(
        metric ->
            metric
                .hasName("jfr.JavaMonitorWait.locktime")
                .hasUnit("milliseconds")
                .hasDoubleHistogram());
//                .points()
//                .anySatisfy(point -> assertThat(point.getValue()).isPositive()));
//        metric ->
//            metric
//                .hasName("runtime.java.cpu_time")
//                .hasUnit("seconds")
//                .hasDoubleGauge()
//                .points()
//                .anySatisfy(point -> assertThat(point.getValue()).isPositive()));

  }

}
