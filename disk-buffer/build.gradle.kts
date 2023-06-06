plugins {
  id("otel.java-conventions")
  id("otel.publish-conventions")
}

description = "Exporter implementations that store signals in disk"
otelJava.moduleName.set("io.opentelemetry.contrib.exporters.storage")

dependencies {
  api("io.opentelemetry:opentelemetry-sdk")
}
