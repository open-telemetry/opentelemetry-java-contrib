plugins {
  id("otel.java-conventions")
  id("otel.publish-conventions")
}

description = "Sampler which makes its decision based on semantic attributes values"
otelJava.moduleName.set("io.opentelemetry.contrib.sampler")

dependencies {
  // TODO: revert versions before merging
  api("io.opentelemetry:opentelemetry-sdk:1.48.0-SNAPSHOT")

  compileOnly("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure-spi:1.48.0-SNAPSHOT")
  compileOnly("io.opentelemetry:opentelemetry-api-incubator:1.48.0-alpha-SNAPSHOT")
  compileOnly("io.opentelemetry:opentelemetry-sdk-extension-incubator:1.48.0-alpha-SNAPSHOT")

  testImplementation("io.opentelemetry.semconv:opentelemetry-semconv-incubating")
  testImplementation("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure:1.48.0-SNAPSHOT")
  testImplementation("io.opentelemetry:opentelemetry-sdk-extension-incubator:1.48.0-alpha-SNAPSHOT")
}
