plugins {
  id("otel.java-conventions")
  id("otel.publish-conventions")
}

description = "zstd compressor implementation for use with OTLP exporters"
otelJava.moduleName.set("io.opentelemetry.contrib.compressor.zstd")

dependencies {
  // TODO(jack-berg): Use version from :depedencyManagement when opentelemetry-instrumentation-bom-alpha depends on opentelemetry-java 1.34.0
  var openTelemetryVersion = "1.37.0"
  api("io.opentelemetry:opentelemetry-exporter-common:$openTelemetryVersion")

  implementation("com.github.luben:zstd-jni:1.5.6-3")

  testImplementation("io.opentelemetry:opentelemetry-sdk-testing:$openTelemetryVersion")
  testImplementation("io.opentelemetry:opentelemetry-exporter-otlp:$openTelemetryVersion")

  testImplementation("io.opentelemetry.proto:opentelemetry-proto")
  testImplementation("com.linecorp.armeria:armeria-junit5")
}
