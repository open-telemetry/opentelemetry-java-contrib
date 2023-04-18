plugins {
  id("otel.java-conventions")
  id("otel.publish-conventions")
}

description = "OpenTelemetry JFR Connection"
otelJava.moduleName.set("io.opentelemetry.contrib.jfr.connection")

dependencies {
  testImplementation("org.openjdk.jmc:common:8.3.1")
  testImplementation("org.openjdk.jmc:flightrecorder:8.3.0")
}
