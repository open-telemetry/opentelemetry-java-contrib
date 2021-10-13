package io.opentelemetry.contrib.jfr.metrics;

import org.testcontainers.junit.jupiter.*;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

//import static org.junit.jupiter.api.Assertions.assertEquals;

@Testcontainers
public class JfrTestReceiver {

    private static final DockerImageName imageName = DockerImageName.parse("redis:6.2.3-alpine");

    @Container
    public static GenericContainer reciver = new GenericContainer(imageName)
        .withExposedPorts(4317);

    // Tests to follow...

}
