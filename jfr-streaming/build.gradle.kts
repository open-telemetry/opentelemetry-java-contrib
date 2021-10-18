val gsonVersion: String by project
val slf4jVersion: String by project
val otelVersion: String by project
val otelAlphaVersion: String by project


plugins {
    id("com.github.johnrengelman.shadow")
    id("java")
    id("otel.java-conventions")
}

repositories {
    mavenCentral()
    mavenLocal()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    implementation("io.opentelemetry:opentelemetry-api")
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
  test {
    useJUnitPlatform()
    dependsOn("shadowJar")
    jvmArgs("-Djdk.attach.allowAttachSelf=true")
  }

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
        "Premain-Class" to "io.opentelemetry.contrib.jfr.Agent",
        "Agent-Class" to "io.opentelemetry.contrib.jfr.Agent",
        "Implementation-Version" to project.version,
        "Implementation-Vendor" to "Open Telemetry"
      )
    }
  }

  named("assemble") {
    dependsOn("shadowJar")
  }
}