plugins {
  id("otel.java-conventions")
  id("com.google.protobuf") version "0.9.4"
}

description = "Client implementation of the OpAMP spec."
otelJava.moduleName.set("io.opentelemetry.contrib.opamp.client")

java {
  sourceCompatibility = JavaVersion.VERSION_1_8
  targetCompatibility = JavaVersion.VERSION_1_8
}

sourceSets {
  main {
    proto {
      srcDir("opamp-spec/proto")
    }
  }
}

tasks.withType<JavaCompile>().configureEach {
  with(options) {
    // Suppressing warnings about the usage of deprecated methods.
    // This is needed because the Protobuf plugin (com.google.protobuf) generates code that uses deprecated methods.
    compilerArgs.add("-Xlint:-deprecation")
  }
}

val protobufVersion = "4.28.2"

protobuf {
  protoc {
    artifact = "com.google.protobuf:protoc:$protobufVersion"
  }
}

dependencies {
  implementation("com.google.protobuf:protobuf-java:$protobufVersion")
  annotationProcessor("com.google.auto.value:auto-value")
  compileOnly("com.google.auto.value:auto-value-annotations")
}
