import net.ltgt.gradle.errorprone.errorprone

plugins {
  id("otel.java-conventions")
  id("otel.publish-conventions")
}

description = "OpenTelemetry Java profiling based inferred spans module"
otelJava.moduleName.set("io.opentelemetry.contrib.inferredspans")

dependencies {
  annotationProcessor("com.google.auto.service:auto-service")
  compileOnly("com.google.auto.service:auto-service-annotations")
  compileOnly("io.opentelemetry:opentelemetry-sdk")
  compileOnly("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure-spi")
  implementation("com.lmax:disruptor")
  implementation("org.jctools:jctools-core")
  implementation("tools.profiler:async-profiler")
  implementation("com.blogspot.mydailyjava:weak-lock-free")
  implementation("org.agrona:agrona")

  testAnnotationProcessor("com.google.auto.service:auto-service")
  testCompileOnly("com.google.auto.service:auto-service-annotations")
  testImplementation("io.opentelemetry.semconv:opentelemetry-semconv-incubating")
  testImplementation("io.opentelemetry:opentelemetry-sdk")
  testImplementation("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")
  testImplementation("io.opentelemetry:opentelemetry-sdk-testing")
  testImplementation("io.opentelemetry:opentelemetry-api-incubator")
  testImplementation("io.opentelemetry:opentelemetry-exporter-logging")
}

tasks {
  withType<JavaCompile>().configureEach {
    with(options) {
      errorprone {
        // This code uses nullable reference in many places due to performance
        // and makes assumptions of when these references are non-null
        // In the code we express those assumptions as assertions
        // instead of Object.requireNonNull because the NPEs raised by actual
        // null dereferencing are more helpful than the ones raised by Object.requireNonNull
        option("NullAway:AssertsEnabled", "true")
      }
    }
  }

  withType<Test>().configureEach {
    jvmArgs("-Djava.util.logging.config.file=${project.projectDir.resolve("src/test/resources/logging.properties")}")
  }
}
