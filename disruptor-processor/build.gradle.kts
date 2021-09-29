plugins {
  id("otel.java-conventions")
  id("otel.publish-conventions")
}

description = "OpenTelemetry Disruptor Processor"

dependencies {
  api("io.opentelemetry:opentelemetry-api")
  api("io.opentelemetry:opentelemetry-sdk")

  implementation("com.google.guava:guava")
  implementation("com.lmax:disruptor:3.4.4")
}
