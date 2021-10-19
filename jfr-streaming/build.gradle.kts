plugins {
  id("otel.java-conventions")
}

dependencies {
  implementation("io.opentelemetry:opentelemetry-api-metrics")

  testImplementation("io.opentelemetry:opentelemetry-sdk-metrics-testing")
  testImplementation("io.opentelemetry:opentelemetry-sdk-testing")
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

  test {
    useJUnitPlatform()
  }
}
