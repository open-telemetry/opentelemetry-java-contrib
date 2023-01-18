plugins {
  id("otel.java-conventions")
  id("otel.publish-conventions")
}

description = "OpenTelemetry Noop API"

dependencies {
  api("io.opentelemetry:opentelemetry-api")
}
