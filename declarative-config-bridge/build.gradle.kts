plugins {
  id("otel.java-conventions")
  id("otel.publish-conventions")
}

description = "OpenTelemetry extension that provides a bridge for declarative configuration."
otelJava.moduleName.set("io.opentelemetry.contrib.sdk.config.bridge")

dependencies {
  // We use `compileOnly` dependency because during runtime all necessary classes are provided by
  // javaagent itself.
  compileOnly("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")
  compileOnly("io.opentelemetry:opentelemetry-sdk-extension-incubator")

  testImplementation("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")
  testImplementation("io.opentelemetry:opentelemetry-sdk-extension-incubator")
  testImplementation("org.mockito:mockito-inline")
}
