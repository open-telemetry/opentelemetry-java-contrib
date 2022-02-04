plugins {
  id("otel.java-conventions")
  id("otel.publish-conventions")
}

description = "Sampler and exporter implementations for consistent sampling"

dependencies {
  api("io.opentelemetry:opentelemetry-sdk")
  testImplementation("com.google.guava:guava:31.0.1-jre")
  testImplementation("org.hipparchus:hipparchus-core:2.0")
  testImplementation("org.hipparchus:hipparchus-stat:2.0")
}
