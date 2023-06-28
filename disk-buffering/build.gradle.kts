plugins {
  id("otel.java-conventions")
  id("otel.publish-conventions")
  id("me.champeau.jmh") version "0.7.1"
}

description = "Exporter implementations that store signals on disk"
otelJava.moduleName.set("io.opentelemetry.contrib.exporters.disk")

java {
  sourceCompatibility = JavaVersion.VERSION_1_8
  targetCompatibility = JavaVersion.VERSION_1_8
}

val mapStructVersion = "1.5.5.Final"
val autovalueVersion = "1.10.1"
dependencies {
  api("io.opentelemetry:opentelemetry-sdk")
  implementation("io.opentelemetry:opentelemetry-exporter-otlp-common")
  implementation("io.opentelemetry.proto:opentelemetry-proto:0.20.0-alpha")
  implementation("org.mapstruct:mapstruct:$mapStructVersion")
  compileOnly("com.google.auto.value:auto-value-annotations:$autovalueVersion")
  annotationProcessor("com.google.auto.value:auto-value:$autovalueVersion")
  annotationProcessor("org.mapstruct:mapstruct-processor:$mapStructVersion")
  testImplementation("org.mockito:mockito-inline:4.11.0")
  testImplementation("io.opentelemetry:opentelemetry-sdk-testing")
}

jmh {
  warmupIterations.set(0)
  fork.set(2)
  iterations.set(5)
  timeOnIteration.set("5s")
  timeUnit.set("ms")
}
