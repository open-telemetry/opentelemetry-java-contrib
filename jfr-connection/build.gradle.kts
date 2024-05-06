plugins {
  id("otel.java-conventions")
  id("otel.publish-conventions")
}

description = "OpenTelemetry JFR Connection"
otelJava.moduleName.set("io.opentelemetry.contrib.jfr.connection")

dependencies {
  testImplementation("org.openjdk.jmc:common:8.3.1")
  testImplementation("org.openjdk.jmc:flightrecorder:9.0.0")
}

tasks {
  test {
    val testJavaVersion: String? by project
    //  jmc libraries used in test code require 17+ now
    if (listOf("8", "11").contains(testJavaVersion)) {
      enabled = false
    }
  }
}
