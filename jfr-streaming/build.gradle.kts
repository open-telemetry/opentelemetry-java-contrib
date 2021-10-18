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
//    implementation(platform("io.opentelemetry:opentelemetry-bom:${otelVersion}"))
    implementation("io.opentelemetry:opentelemetry-api")
    implementation("io.opentelemetry:opentelemetry-sdk")
    implementation("io.opentelemetry:opentelemetry-exporter-otlp")
    implementation("io.opentelemetry:opentelemetry-sdk-metrics")

    implementation("io.opentelemetry:opentelemetry-exporter-otlp-metrics:${otelAlphaVersion}")
//    implementation(platform("io.grpc:grpc-bom:1.34.1"))
    implementation("io.grpc:grpc-netty-shaded")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.1")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.7.1")
    testImplementation("org.assertj:assertj-core:3.21.0")
    testImplementation("io.opentelemetry:opentelemetry-sdk-metrics-testing:${otelAlphaVersion}")
    testImplementation("io.opentelemetry:opentelemetry-sdk-testing")

    testImplementation("org.awaitility:awaitility:3.0.0")
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