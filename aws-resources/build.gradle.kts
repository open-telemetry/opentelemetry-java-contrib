plugins {
  id("otel.java-conventions")

  id("otel.publish-conventions")
}

description = "OpenTelemetry AWS Resources Support"

dependencies {
  api("io.opentelemetry:opentelemetry-api")
  api("io.opentelemetry:opentelemetry-sdk")

  implementation("io.opentelemetry:opentelemetry-semconv")

  compileOnly("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")

  implementation("com.fasterxml.jackson.core:jackson-core")
  implementation("com.squareup.okhttp3:okhttp")

  testImplementation("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")
  testImplementation("io.opentelemetry:opentelemetry-sdk-testing")

  testImplementation("com.linecorp.armeria:armeria-junit5")
  testRuntimeOnly("org.bouncycastle:bcpkix-jdk15on")
  testImplementation("com.google.guava:guava")
  testImplementation("org.skyscreamer:jsonassert")
}
