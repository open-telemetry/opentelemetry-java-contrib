plugins {
  id("otel.java-conventions")
  id("otel.publish-conventions")
}

description = "Tools to intercept and process signals globally."
otelJava.moduleName.set("io.opentelemetry.contrib.processors")

java {
  sourceCompatibility = JavaVersion.VERSION_1_8
  targetCompatibility = JavaVersion.VERSION_1_8
}

dependencies {
  api("io.opentelemetry:opentelemetry-sdk")
  testImplementation("io.opentelemetry:opentelemetry-sdk-testing")
}
