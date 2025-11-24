plugins {
  id("otel.java-conventions")
  id("otel.publish-conventions")
}

description = "zstd compressor implementation for use with OTLP exporters"
otelJava.moduleName.set("io.opentelemetry.contrib.compressor.zstd")

dependencies {
  api("io.opentelemetry:opentelemetry-exporter-common")

  implementation("com.github.luben:zstd-jni:1.5.7-6")

  testImplementation("io.opentelemetry:opentelemetry-sdk-testing")
  testImplementation("io.opentelemetry:opentelemetry-exporter-otlp")

  testImplementation("io.opentelemetry.proto:opentelemetry-proto")
  testImplementation("com.linecorp.armeria:armeria-junit5")
}
