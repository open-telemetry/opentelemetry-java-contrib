plugins {
  id("otel.java-conventions")
  id("otel.publish-conventions")
}

description = "OpenTelemetry Java profiling based inferred spans module"
otelJava.moduleName.set("io.opentelemetry.contrib.inferredspans")

dependencies {
  annotationProcessor("com.google.auto.service:auto-service")
  compileOnly("com.google.auto.service:auto-service-annotations")
  compileOnly("io.opentelemetry:opentelemetry-sdk")
  compileOnly("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure-spi")
  implementation("com.lmax:disruptor")
  implementation("org.jctools:jctools-core")
  implementation("tools.profiler:async-profiler")
  implementation("com.blogspot.mydailyjava:weak-lock-free")
  implementation("org.agrona:agrona")
  // implementation(libs.bundles.semconv)

  testAnnotationProcessor("com.google.auto.service:auto-service")
  testCompileOnly("com.google.auto.service:auto-service-annotations")
  testImplementation("io.opentelemetry:opentelemetry-sdk")
  testImplementation("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")
  testImplementation("io.opentelemetry:opentelemetry-sdk-testing")
  testImplementation("io.opentelemetry:opentelemetry-api-incubator")
  testImplementation("io.opentelemetry:opentelemetry-exporter-logging")
  // testImplementation("org.awaitility:awaitility")
  // testImplementation(libs.bundles.semconv)
}
