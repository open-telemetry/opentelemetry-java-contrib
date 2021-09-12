plugins {
  id("java")
  id("com.github.johnrengelman.shadow")
  id("otel.java-conventions")
  id("otel.publish-conventions")
}

// NOTE
// `META-INF/plexus/components.xml` is manually handled under src/main/resources because there is no Gradle
// equivalent to the Maven plugin `plexus-component-metadata:generate-metadata`

description = "Maven extension to observe Maven builds with distributed traces using OpenTelemetry SDK"

dependencies {
  implementation("org.codehaus.plexus:plexus-component-annotations:2.1.0")

  implementation("io.opentelemetry:opentelemetry-api")
  implementation("io.opentelemetry:opentelemetry-sdk")
  implementation("io.opentelemetry:opentelemetry-sdk-trace")
  implementation("io.opentelemetry:opentelemetry-semconv")
  implementation("io.opentelemetry:opentelemetry-exporter-otlp")
  implementation("io.opentelemetry:opentelemetry-exporter-otlp-trace")

  implementation("io.grpc:grpc-netty-shaded")

  annotationProcessor("com.google.auto.value:auto-value")
  compileOnly("com.google.auto.value:auto-value-annotations")
  
  compileOnly("org.apache.maven:maven-core:3.5.0")
  compileOnly("org.slf4j:slf4j-api")
  compileOnly("org.sonatype.aether:aether-api:1.13.1")


  testImplementation("org.apache.maven:maven-core:3.5.0")
  testImplementation("org.slf4j:slf4j-simple")
}

tasks {
  shadowJar {
    archiveClassifier.set("")
  }

  assemble {
    dependsOn(shadowJar)
  }
}

