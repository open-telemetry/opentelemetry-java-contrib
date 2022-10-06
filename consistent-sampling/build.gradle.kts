plugins {
  id("otel.java-conventions")
  id("otel.publish-conventions")
}

description = "Sampler and exporter implementations for consistent sampling"

dependencies {
  api("io.opentelemetry:opentelemetry-sdk-trace")
  testImplementation("org.hipparchus:hipparchus-core:2.2")
  testImplementation("org.hipparchus:hipparchus-stat:2.3")
}
