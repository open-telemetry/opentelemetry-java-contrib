plugins {
  id("otel.java-conventions")

  id("otel.publish-conventions")
}

description = "OpenTelemetry AWS X-Ray Propagator"
otelJava.moduleName.set("io.opentelemetry.contrib.awsxray.propagator")

dependencies {
  // TODO: revert versions before merging
  api("io.opentelemetry:opentelemetry-api:1.48.0-SNAPSHOT")
  compileOnly("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure-spi:1.48.0-SNAPSHOT")
  compileOnly("io.opentelemetry:opentelemetry-api-incubator:1.48.0-alpha-SNAPSHOT")
  testImplementation("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure:1.48.0-SNAPSHOT")
  testImplementation("io.opentelemetry:opentelemetry-sdk-trace:1.48.0-SNAPSHOT")
  testImplementation("io.opentelemetry:opentelemetry-sdk-testing:1.48.0-SNAPSHOT")

  testImplementation("io.opentelemetry:opentelemetry-sdk-extension-incubator:1.48.0-alpha-SNAPSHOT")
  testImplementation("uk.org.webcompere:system-stubs-jupiter:2.0.3")
}
