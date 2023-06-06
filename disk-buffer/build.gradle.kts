plugins {
  id("otel.java-conventions")
  id("otel.publish-conventions")
}

description = "Exporter implementations that store signals in disk"
otelJava.moduleName.set("io.opentelemetry.contrib.exporters.storage")

val dslJsonVersion = "1.10.0"
dependencies {
  api("io.opentelemetry:opentelemetry-sdk")
  implementation("com.dslplatform:dsl-json-java8:$dslJsonVersion")
  annotationProcessor("com.dslplatform:dsl-json-java8:$dslJsonVersion")
}
