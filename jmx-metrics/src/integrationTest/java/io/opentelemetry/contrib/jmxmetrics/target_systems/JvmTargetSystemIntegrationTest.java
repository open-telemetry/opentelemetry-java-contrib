package io.opentelemetry.contrib.jmxmetrics.target_systems;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.contrib.jmxmetrics.AbstractIntegrationTest;
import io.opentelemetry.proto.metrics.v1.Metric;
import io.opentelemetry.proto.metrics.v1.NumberDataPoint;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.MountableFile;

class JvmTargetSystemIntegrationTest extends AbstractIntegrationTest {

  JvmTargetSystemIntegrationTest() {
    super(false, "target-systems/jvm.properties");
  }

  @Container
  GenericContainer<?> cassandra =
      new GenericContainer<>("cassandra:3.11")
          .withNetwork(Network.SHARED)
          .withEnv("LOCAL_JMX", "no")
          .withCopyFileToContainer(
              MountableFile.forClasspathResource("cassandra/jmxremote.password", 0400),
              "/etc/cassandra/jmxremote.password")
          .withNetworkAliases("cassandra")
          .withExposedPorts(7199)
          .withStartupTimeout(Duration.ofSeconds(120))
          .waitingFor(Wait.forListeningPort());

  @Test
  void endToEnd() {
    waitAndAssertMetrics(
        metric -> assertGauge(metric, "jvm.classes.loaded", "number of loaded classes", "1"),
        metric ->
            assertTypedSum(
                metric,
                "jvm.gc.collections.count",
                "total number of collections that have occurred",
                "1",
                Arrays.asList("ConcurrentMarkSweep", "ParNew")),
        metric ->
            assertTypedSum(
                metric,
                "jvm.gc.collections.elapsed",
                "the approximate accumulated collection elapsed time in milliseconds",
                "ms",
                Arrays.asList("ConcurrentMarkSweep", "ParNew")),
        metric -> assertGauge(metric, "jvm.memory.heap.committed", "current heap usage", "by"),
        metric -> assertGauge(metric, "jvm.memory.heap.init", "current heap usage", "by"),
        metric -> assertGauge(metric, "jvm.memory.heap.max", "current heap usage", "by"),
        metric -> assertGauge(metric, "jvm.memory.heap.used", "current heap usage", "by"),
        metric ->
            assertGauge(metric, "jvm.memory.nonheap.committed", "current non-heap usage", "by"),
        metric -> assertGauge(metric, "jvm.memory.nonheap.init", "current non-heap usage", "by"),
        metric -> assertGauge(metric, "jvm.memory.nonheap.max", "current non-heap usage", "by"),
        metric -> assertGauge(metric, "jvm.memory.nonheap.used", "current non-heap usage", "by"),
        metric ->
            assertTypedGauge(
                metric,
                "jvm.memory.pool.committed",
                "current memory pool usage",
                "by",
                Arrays.asList(
                    "Code Cache",
                    "Par Eden Space",
                    "CMS Old Gen",
                    "Compressed Class Space",
                    "Metaspace",
                    "Par Survivor Space")),
        metric ->
            assertTypedGauge(
                metric,
                "jvm.memory.pool.init",
                "current memory pool usage",
                "by",
                Arrays.asList(
                    "Code Cache",
                    "Par Eden Space",
                    "CMS Old Gen",
                    "Compressed Class Space",
                    "Metaspace",
                    "Par Survivor Space")),
        metric ->
            assertTypedGauge(
                metric,
                "jvm.memory.pool.max",
                "current memory pool usage",
                "by",
                Arrays.asList(
                    "Code Cache",
                    "Par Eden Space",
                    "CMS Old Gen",
                    "Compressed Class Space",
                    "Metaspace",
                    "Par Survivor Space")),
        metric ->
            assertTypedGauge(
                metric,
                "jvm.memory.pool.used",
                "current memory pool usage",
                "by",
                Arrays.asList(
                    "Code Cache",
                    "Par Eden Space",
                    "CMS Old Gen",
                    "Compressed Class Space",
                    "Metaspace",
                    "Par Survivor Space")),
        metric -> assertGauge(metric, "jvm.threads.count", "number of threads", "1"));
  }

  private static void assertTypedGauge(
      Metric metric, String name, String description, String unit, List<String> types) {
    assertThat(metric.getName()).isEqualTo(name);
    assertThat(metric.getDescription()).isEqualTo(description);
    assertThat(metric.getUnit()).isEqualTo(unit);
    assertThat(metric.hasGauge()).isTrue();
    assertTypedPoints(metric.getGauge().getDataPointsList(), types);
  }

  private static void assertTypedSum(
      Metric metric, String name, String description, String unit, List<String> types) {
    assertThat(metric.getName()).isEqualTo(name);
    assertThat(metric.getDescription()).isEqualTo(description);
    assertThat(metric.getUnit()).isEqualTo(unit);
    assertThat(metric.hasSum()).isTrue();
    assertTypedPoints(metric.getSum().getDataPointsList(), types);
  }

  @SuppressWarnings("unchecked")
  private static void assertTypedPoints(List<NumberDataPoint> points, List<String> types) {
    assertThat(points)
        .satisfiesExactlyInAnyOrder(
            types.stream()
                .map(
                    type ->
                        (Consumer<NumberDataPoint>)
                            point ->
                                assertThat(point.getAttributesList())
                                    .singleElement()
                                    .satisfies(
                                        attribute -> {
                                          assertThat(attribute.getKey()).isEqualTo("name");
                                          assertThat(attribute.getValue().getStringValue())
                                              .isEqualTo(type);
                                        }))
                .toArray(Consumer[]::new));
  }
}
