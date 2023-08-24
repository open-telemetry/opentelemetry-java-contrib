plugins {
  id("otel.java-conventions")

  id("otel.publish-conventions")
}

description = "OpenTelemetry AWS X-Ray Propagator"
otelJava.moduleName.set("io.opentelemetry.contrib.awsxray.propagator")

dependencies {
  api("io.opentelemetry:opentelemetry-api")
  compileOnly("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure-spi")
  testImplementation("uk.org.webcompere:system-stubs-jupiter:2.0.2")
}
