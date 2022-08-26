plugins {
  id("otel.java-conventions")
  id("otel.publish-conventions")
}

description = "Assorted ResourceProvider implementations."

dependencies {
  api("io.opentelemetry:opentelemetry-sdk")
  api("io.opentelemetry:opentelemetry-semconv")

  annotationProcessor("com.google.auto.service:auto-service")
  compileOnly("com.google.auto.service:auto-service-annotations")
  implementation("com.google.auto.service:auto-service")

  implementation("org.yaml:snakeyaml:1.30")
  implementation("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")
}

tasks {
  compileJava {
    options.release.set(8)
  }
  compileTestJava {
    options.release.set(11)
  }
}
