plugins {
  id("otel.java-conventions")
  id("otel.publish-conventions")
}

description = "Sampler and exporter implementations for consistent sampling"
otelJava.moduleName.set("io.opentelemetry.contrib.sampler.consistent")

dependencies {
  api("io.opentelemetry:opentelemetry-sdk-trace")
  testImplementation("org.hipparchus:hipparchus-core:2.3")
  testImplementation("org.hipparchus:hipparchus-stat:2.3")
}
