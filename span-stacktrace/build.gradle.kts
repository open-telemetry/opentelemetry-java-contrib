plugins {
  id("otel.java-conventions")
}

description = "OpenTelemetry Java span stacktrace capture module"

dependencies {
  api("io.opentelemetry:opentelemetry-sdk")
  testImplementation("io.opentelemetry:opentelemetry-sdk-testing")

  implementation("io.opentelemetry.semconv:opentelemetry-semconv-incubating:1.25.0-alpha")
}
