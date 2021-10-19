plugins {
  id("com.github.johnrengelman.shadow")
  id("otel.java-conventions")
}

dependencies {
  implementation("io.opentelemetry:opentelemetry-api-metrics")
  implementation("io.opentelemetry:opentelemetry-sdk")
  implementation("io.opentelemetry:opentelemetry-exporter-otlp")
  implementation("io.opentelemetry:opentelemetry-sdk-metrics")
  implementation("io.opentelemetry:opentelemetry-exporter-otlp-metrics")
  implementation("io.grpc:grpc-netty-shaded")

  testImplementation("org.junit.jupiter:junit-jupiter-api")
  testImplementation("org.junit.jupiter:junit-jupiter-engine")
  testImplementation("org.assertj:assertj-core")
  testImplementation("io.opentelemetry:opentelemetry-sdk-metrics-testing")
  testImplementation("io.opentelemetry:opentelemetry-sdk-testing")

  testImplementation("org.awaitility:awaitility")
}

tasks {
  withType(JavaCompile::class) {
    options.release.set(17)
  }

  withType<Javadoc>().configureEach {
    with(options as StandardJavadocDocletOptions) {
      source = "17"
    }
  }

  shadowJar {
    archiveClassifier.set("")
    manifest {
      attributes(
        "Implementation-Version" to project.version,
        "Implementation-Vendor" to "Open Telemetry"
      )
    }
  }

  test {
    useJUnitPlatform()
  }

  named("assemble") {
    dependsOn("shadowJar")
  }
}
