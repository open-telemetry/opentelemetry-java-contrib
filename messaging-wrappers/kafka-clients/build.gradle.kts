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
  testImplementation(project(":messaging-wrappers:testing"))

  testAnnotationProcessor("com.google.auto.service:auto-service")
  testCompileOnly("com.google.auto.service:auto-service-annotations")
  testImplementation("org.testcontainers:kafka")
  testImplementation("org.testcontainers:junit-jupiter")
}

tasks {
  withType<Test>().configureEach {
    jvmArgs("-Dotel.java.global-autoconfigure.enabled=true")
    // TODO: According to https://opentelemetry.io/docs/specs/semconv/messaging/messaging-spans/#message-creation-context-as-parent-of-process-span,
    //  process span should be the child of receive span. However, we couldn't access the trace context with receive span
    //  in wrappers, unless we add a generic accessor for that.
    jvmArgs("-Dotel.instrumentation.messaging.experimental.receive-telemetry.enabled=false")
    jvmArgs("-Dotel.traces.exporter=logging")
    jvmArgs("-Dotel.metrics.exporter=logging")
    jvmArgs("-Dotel.logs.exporter=logging")
  }
}
