plugins {
  id("otel.java-conventions")
  id("otel.publish-conventions")
}

description = "Sampler which makes its decision based on semantic attributes values"
otelJava.moduleName.set("io.opentelemetry.contrib.sampler")

dependencies {
  api("io.opentelemetry:opentelemetry-sdk")
  api("io.opentelemetry.semconv:opentelemetry-semconv")
  testImplementation("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")
  api("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure-spi")
}
