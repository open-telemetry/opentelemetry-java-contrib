plugins {
  id("com.github.johnrengelman.shadow")

  id("otel.java-conventions")
  id("otel.publish-conventions")
}

description = "Sampler which makes its decision based on http.url semantic attribute"

repositories {
  mavenCentral()
}

dependencies {
  implementation("io.opentelemetry:opentelemetry-sdk")
  implementation("io.opentelemetry:opentelemetry-semconv")
  implementation("org.slf4j:slf4j-simple")
}

tasks {
  shadowJar {
    // This should always be standalone, so remove "-all" to prevent unnecessary artifact.
    archiveClassifier.set("")
  }

}
