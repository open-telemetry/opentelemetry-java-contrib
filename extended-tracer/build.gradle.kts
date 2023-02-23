plugins {
  id("otel.java-conventions")
  id("otel.publish-conventions")
}

description = "Tracing Utilities"
otelJava.moduleName.set("io.opentelemetry.contrib.extended-tracer")

dependencies {
  api("io.opentelemetry:opentelemetry-api")
}
