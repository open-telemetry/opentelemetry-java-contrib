plugins {
  id("otel.java-conventions")
  id("otel.publish-conventions")
}

description = "Sampler which makes its decision based on semantic attributes values"
otelJava.moduleName.set("io.opentelemetry.contrib.sampler.cel")

dependencies {
  api("io.opentelemetry:opentelemetry-sdk")

  implementation("dev.cel:cel:0.15.0")
  // can be removed when cel updates the guava version
  implementation("com.google.guava:guava") {
    because("Versions before 33.7.2 are affected by CVE-2026-102554")
  }

  compileOnly("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure-spi")
  compileOnly("io.opentelemetry:opentelemetry-sdk-extension-declarative-config")

  testImplementation("io.opentelemetry.semconv:opentelemetry-semconv-incubating")
  testImplementation("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")
  testImplementation("io.opentelemetry:opentelemetry-sdk-extension-declarative-config")
}
