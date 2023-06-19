plugins {
  id("otel.java-conventions")
  id("otel.publish-conventions")
}

description = "Exporter implementations that store signals in disk"
otelJava.moduleName.set("io.opentelemetry.contrib.disk.buffering")

tasks {
  withType<JavaCompile>().configureEach {
    with(options) {
      compilerArgs.addAll(listOf("-Xlint:-unchecked", "-Xlint:-rawtypes"))
    }
  }
}

java {
  sourceCompatibility = JavaVersion.VERSION_1_8
  targetCompatibility = JavaVersion.VERSION_1_8
}

val dslJsonVersion = "1.10.0"
val mapStructVersion = "1.5.5.Final"
val autovalueVersion = "1.10.1"
dependencies {
  api("io.opentelemetry:opentelemetry-sdk")
  implementation("com.dslplatform:dsl-json-java8:$dslJsonVersion")
  implementation("io.opentelemetry.proto:opentelemetry-proto:0.20.0-alpha")
  compileOnly("org.mapstruct:mapstruct:$mapStructVersion")
  compileOnly("com.google.auto.value:auto-value-annotations:$autovalueVersion")
  annotationProcessor("com.google.auto.value:auto-value:$autovalueVersion")
  annotationProcessor("com.dslplatform:dsl-json-java8:$dslJsonVersion")
  annotationProcessor("org.mapstruct:mapstruct-processor:$mapStructVersion")
  testImplementation("org.mockito:mockito-inline:4.11.0")
}
