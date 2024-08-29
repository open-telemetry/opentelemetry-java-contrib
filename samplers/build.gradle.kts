plugins {
  id("otel.java-conventions")
  id("otel.publish-conventions")
}

description = "Sampler which makes its decision based on semantic attributes values"
otelJava.moduleName.set("io.opentelemetry.contrib.sampler")

dependencies {
  // TODO: remove SNAPSHOT dependencies before merging
  api("io.opentelemetry:opentelemetry-sdk:1.42.0-SNAPSHOT")
  api("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure-spi:1.42.0-SNAPSHOT")

  compileOnly("io.opentelemetry:opentelemetry-sdk-extension-incubator:1.42.0-alpha-SNAPSHOT")

  implementation("io.opentelemetry.semconv:opentelemetry-semconv")

  testImplementation("io.opentelemetry.semconv:opentelemetry-semconv-incubating")
  testImplementation("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")
  testImplementation("io.opentelemetry:opentelemetry-sdk-extension-incubator:1.42.0-alpha-SNAPSHOT")
}
