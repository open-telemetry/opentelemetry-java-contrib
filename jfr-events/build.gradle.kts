plugins {
  id("otel.java-conventions")
  id("otel.publish-conventions")
}

description = "OpenTelemetry JFR Events"
otelJava.moduleName.set("io.opentelemetry.contrib.jfrevent")

dependencies {
  implementation("io.opentelemetry:opentelemetry-sdk")
}

tasks {
  withType(JavaCompile::class) {
    options.release.set(11)
  }

  test {
    val testJavaVersion: String? by project
    if (testJavaVersion == "8") {
      enabled = false
    }

    // Disabled due to https://bugs.openjdk.java.net/browse/JDK-8245283
    configure<JacocoTaskExtension> {
      enabled = false
    }
  }
}
