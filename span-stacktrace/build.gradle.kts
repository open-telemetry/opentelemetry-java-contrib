plugins {
  id("otel.java-conventions")
}

description = "OpenTelemetry Java span stacktrace capture module"

dependencies {
  api("io.opentelemetry:opentelemetry-sdk")
  testImplementation("io.opentelemetry:opentelemetry-sdk-testing")
}
