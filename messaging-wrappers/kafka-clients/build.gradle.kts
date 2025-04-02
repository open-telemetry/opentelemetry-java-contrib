plugins {
  id("otel.java-conventions")

  id("otel.publish-conventions")
}

description = "OpenTelemetry Messaging Wrappers - kafka-clients implementation"
otelJava.moduleName.set("io.opentelemetry.contrib.messaging.wrappers.kafka")

dependencies {
  api(project(":messaging-wrappers:api"))

  // FIXME: We shouldn't depend on the library "opentelemetry-kafka-clients-common" directly because the api in this
  //  package could be mutable, unless the components were maintained in "opentelemetry-java-instrumentation" project.
  // implementation("io.opentelemetry.instrumentation:opentelemetry-kafka-clients-common:2.13.3-alpha")

  compileOnly("org.apache.kafka:kafka-clients:0.11.0.0")

  testImplementation("org.apache.kafka:kafka-clients:0.11.0.0")
  testImplementation("io.opentelemetry.instrumentation:opentelemetry-kafka-clients-2.6")

  testAnnotationProcessor("com.google.auto.service:auto-service")
  testCompileOnly("com.google.auto.service:auto-service-annotations")
  testImplementation("org.junit.jupiter:junit-jupiter-api")
  testImplementation("org.junit.jupiter:junit-jupiter-params")
  testImplementation("org.testcontainers:kafka")
  testImplementation("org.testcontainers:junit-jupiter")

  testImplementation("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")
  testImplementation("io.opentelemetry:opentelemetry-sdk")
  testImplementation("io.opentelemetry:opentelemetry-sdk-testing")
  testImplementation("io.opentelemetry:opentelemetry-sdk-extension-incubator")
  testImplementation("io.opentelemetry.semconv:opentelemetry-semconv")
  testImplementation("io.opentelemetry.semconv:opentelemetry-semconv-incubating")
  testImplementation("io.opentelemetry:opentelemetry-exporter-logging")
  testImplementation("io.opentelemetry.javaagent:opentelemetry-testing-common")
}

tasks {
  withType<Test>().configureEach {
    jvmArgs("-Dotel.java.global-autoconfigure.enabled=true")
    jvmArgs("-Dotel.instrumentation.messaging.experimental.receive-telemetry.enabled=true")
    jvmArgs("-Dotel.traces.exporter=logging")
    jvmArgs("-Dotel.metrics.exporter=logging")
    jvmArgs("-Dotel.logs.exporter=logging")
  }
}