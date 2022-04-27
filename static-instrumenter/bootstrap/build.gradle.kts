plugins {
  id("otel.java-conventions")
}

description = "Bootstrap classes for static agent"

tasks {
  withType<JavaCompile>().configureEach {
    with(options) {
      release.set(11)
    }
  }
}
