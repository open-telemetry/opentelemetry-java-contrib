plugins {
  id("otel.java-conventions")
  id("otel.publish-conventions")
}

description = "OpenTelemetry Java span stacktrace capture module"
otelJava.moduleName.set("io.opentelemetry.contrib.stacktrace")

dependencies {
  api("io.opentelemetry:opentelemetry-sdk")
  testImplementation("io.opentelemetry:opentelemetry-sdk-testing")

  testImplementation("io.opentelemetry.semconv:opentelemetry-semconv-incubating")
}
