plugins {
  id("otel.java-conventions")
  id("otel.publish-conventions")
}

description = "OpenTelemetry Noop API"
otelJava.moduleName.set("io.opentelemetry.contrib.noopapi")

dependencies {
  api("io.opentelemetry:opentelemetry-api")
}
