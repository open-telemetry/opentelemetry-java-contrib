plugins {
  id("otel.java-conventions")

  id("otel.publish-conventions")
}

description = "OpenTelemetry Messaging Wrappers"
otelJava.moduleName.set("io.opentelemetry.contrib.messaging.wrappers")

dependencies {
  api("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api-incubator")

  compileOnly("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure-spi")
  compileOnly("io.opentelemetry:opentelemetry-api-incubator")

  implementation("io.opentelemetry.semconv:opentelemetry-semconv")
  implementation("io.opentelemetry.semconv:opentelemetry-semconv-incubating")

  testImplementation("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")
  testImplementation("io.opentelemetry:opentelemetry-sdk-trace")
  testImplementation("io.opentelemetry:opentelemetry-sdk-testing")

  testImplementation("io.opentelemetry:opentelemetry-sdk-extension-incubator")
  testImplementation("uk.org.webcompere:system-stubs-jupiter:2.0.3")
}
