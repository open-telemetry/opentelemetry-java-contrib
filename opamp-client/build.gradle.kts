plugins {
  id("otel.java-conventions")
}

description = "Client implementation of the OpAMP spec."
otelJava.moduleName.set("io.opentelemetry.contrib.opamp.client")

java {
  sourceCompatibility = JavaVersion.VERSION_1_8
  targetCompatibility = JavaVersion.VERSION_1_8
}
