package io.opentelemetry.contrib.jfr.metrics;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;
import java.time.Duration;

//import static org.junit.jupiter.api.Assertions.assertEquals;

@Testcontainers
public class JfrTestReceiver {

    private static final DockerImageName imageName = DockerImageName.parse("redis:6.2.3-alpine");

    @Container
    public static GenericContainer reciver = new GenericContainer(imageName)
        .withExposedPorts(4317)
        .withStartupTimeout(Duration.ofSeconds(120))
        .waitingFor(Wait.forListeningPort());


  @Test
  void endToEnd() {
//    waitAndAssertMetrics(
//        metric -> {
//          assertThat(metric.getName()).isEqualTo("cassandra.storage.load");
//          assertThat(metric.getDescription())
//              .isEqualTo("Size, in bytes, of the on disk data size this node manages");
//          assertThat(metric.getUnit()).isEqualTo("By");
//          assertThat(metric.hasHistogram()).isTrue();
//          assertThat(metric.getHistogram().getDataPointsList())
//              .satisfiesExactly(
//                  point ->
//                      assertThat(point.getAttributesList())
//                          .containsExactly(
//                              KeyValue.newBuilder()
//                                  .setKey("myKey")
//                                  .setValue(AnyValue.newBuilder().setStringValue("myVal"))
//                                  .build()));
//        });
  }

}
