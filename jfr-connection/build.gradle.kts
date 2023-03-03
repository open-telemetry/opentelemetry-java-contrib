plugins {
  id("otel.java-conventions")
  id("otel.publish-conventions")
}

description = "OpenTelemetry JFR Connection"
otelJava.moduleName.set("io.opentelemetry.contrib.jfr.connection")

dependencies {
  testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")
  testImplementation("org.junit.platform:junit-platform-commons:1.9.2")
  testImplementation("org.openjdk.jmc:common:8.3.0")
  testImplementation("org.openjdk.jmc:flightrecorder:8.3.0")
}

tasks {
  test {
    useJUnitPlatform()
  }
}
