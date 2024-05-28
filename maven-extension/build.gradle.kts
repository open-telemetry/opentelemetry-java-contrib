plugins {
  id("java")
  id("com.github.johnrengelman.shadow")
  id("otel.java-conventions")
  id("otel.publish-conventions")
}

// NOTE
// `META-INF/sis/javax.inject.Named` is manually handled under src/main/resources because there is
// no Gradle equivalent to the Maven plugin `org.eclipse.sisu:sisu-maven-plugin`

description = "Maven extension to observe Maven builds with distributed traces using OpenTelemetry SDK"
otelJava.moduleName.set("io.opentelemetry.maven")

dependencies {
  compileOnly("javax.inject:javax.inject:1")

  implementation("io.opentelemetry:opentelemetry-api")
  implementation("io.opentelemetry:opentelemetry-sdk")
  implementation("io.opentelemetry:opentelemetry-sdk-trace")
  implementation("io.opentelemetry:opentelemetry-sdk-metrics")
  implementation("io.opentelemetry:opentelemetry-sdk-logs")
  implementation("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure-spi")
  implementation("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")
  implementation("io.opentelemetry:opentelemetry-exporter-otlp")
  implementation("io.opentelemetry.semconv:opentelemetry-semconv")
  implementation("io.opentelemetry.semconv:opentelemetry-semconv-incubating:1.25.0-alpha")

  annotationProcessor("com.google.auto.value:auto-value")
  compileOnly("com.google.auto.value:auto-value-annotations")

  compileOnly("org.apache.maven:maven-core:3.5.0") // do not auto-update, support older mvn versions
  compileOnly("org.slf4j:slf4j-api")

  testImplementation("org.apache.maven:maven-core:3.5.0")
  testImplementation("org.slf4j:slf4j-simple")
}

// The jar dependencies bundled in the uber-jar by the shadow plugin are wrongly added as
// 'runtime' dependencies in the generated pom.xml instead of being absent this pom.xml.
// Remove those runtime dependencies from the pom.xml.
configure<PublishingExtension> {
  (components["java"] as AdhocComponentWithVariants).run {
    withVariantsFromConfiguration(configurations["runtimeElements"]) {
      skip()
    }
  }
}

tasks {
  shadowJar {
    manifest {
      attributes["Implementation-Version"] = project.version
    }
    archiveClassifier.set("")
  }

  assemble {
    dependsOn(shadowJar)
  }
}

tasks.getByName("test").dependsOn("shadowJar")
