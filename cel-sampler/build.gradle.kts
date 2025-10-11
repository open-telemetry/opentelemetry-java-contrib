plugins {
  id("otel.java-conventions")
  id("otel.publish-conventions")
}

description = "Sampler which makes its decision based on semantic attributes values"
otelJava.moduleName.set("io.opentelemetry.contrib.sampler.cel")

dependencies {
  api("io.opentelemetry:opentelemetry-sdk")

  implementation("dev.cel:cel:0.11.0")

  compileOnly("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure-spi")
  compileOnly("io.opentelemetry:opentelemetry-sdk-extension-incubator")

  testImplementation("io.opentelemetry.semconv:opentelemetry-semconv-incubating")
  testImplementation("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")
  testImplementation("io.opentelemetry:opentelemetry-sdk-extension-incubator")
}
