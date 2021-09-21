val gsonVersion: String by project
val slf4jVersion: String by project
val otelVersion: String by project
val otelAlphaVersion: String by project


plugins {
    id("com.github.johnrengelman.shadow") version "5.2.0"
    id("java")
<<<<<<< HEAD
//    id("com.github.sherter.google-java-format") version "0.8" apply true
=======
>>>>>>> 7f48f1e (Add to main build)
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
    implementation(platform("io.opentelemetry:opentelemetry-bom:${otelVersion}"))
    implementation("io.opentelemetry:opentelemetry-api")
    implementation("io.opentelemetry:opentelemetry-sdk")
    implementation("io.opentelemetry:opentelemetry-exporter-otlp")
    implementation("io.opentelemetry:opentelemetry-sdk-metrics")
    testImplementation("io.opentelemetry:opentelemetry-sdk-metrics-testing:${otelAlphaVersion}")
    testImplementation("io.opentelemetry:opentelemetry-sdk-testing:${otelVersion}")

    implementation("io.opentelemetry:opentelemetry-exporter-otlp-metrics:${otelAlphaVersion}")
    implementation(platform("io.grpc:grpc-bom:1.34.1"))
    implementation("io.grpc:grpc-netty-shaded")


    testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.7.1")
    testImplementation("org.assertj:assertj-core:3.8.0")

//    testImplementation("org.moditect.jfrunit:jfrunit:1.0.0.Alpha1")

    testImplementation("io.grpc:grpc-api")
    testImplementation("io.grpc:grpc-protobuf")
    testImplementation("io.grpc:grpc-stub")
    testImplementation("io.grpc:grpc-testing")
    testImplementation("org.testcontainers:testcontainers:1.16.0")
    testImplementation("org.testcontainers:junit-jupiter:1.15.3")
    testImplementation("org.awaitility:awaitility:3.0.0")
}

tasks.shadowJar {
    archiveClassifier.set("")
    manifest {
        attributes(
                "Premain-Class" to "org.jfr.Agent",
                "Implementation-Version" to project.version,
                "Implementation-Vendor" to "Open Telemetry"
        )
    }
}

tasks.named("build") {
    dependsOn("shadowJar")
}