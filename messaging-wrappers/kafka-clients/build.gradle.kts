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

  testImplementation("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")
  testImplementation("io.opentelemetry:opentelemetry-sdk-trace")
  testImplementation("io.opentelemetry:opentelemetry-sdk-testing")

  testImplementation("io.opentelemetry:opentelemetry-sdk-extension-incubator")
  testImplementation("uk.org.webcompere:system-stubs-jupiter:2.0.3")
}
