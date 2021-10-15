package io.opentelemetry.contrib.jfrstreaming;

import com.sun.tools.attach.VirtualMachine;
import io.opentelemetry.contrib.jfr.metrics.AbstractMetricsTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.management.ManagementFactory;

import static org.assertj.core.api.Assertions.assertThat;

public class JfrCPUTest extends AbstractMetricsTest {

  @Test
  public void shouldHaveGcAndLockEvents() throws Exception {
    // This should generate some events
    System.gc();
    synchronized (this) {
      Thread.sleep(1000);
    }

    waitAndAssertMetrics(
        metric -> {
          System.err.println(metric);
            metric
                .hasName("jfr.JavaMonitorWait.locktime")
                .hasUnit("milliseconds")
                .hasDoubleHistogram();});
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
