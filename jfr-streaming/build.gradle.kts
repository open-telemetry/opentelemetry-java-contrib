val gsonVersion: String by project
val slf4jVersion: String by project
val otelVersion: String by project
val otelAlphaVersion: String by project


plugins {
    id("com.github.johnrengelman.shadow") version "5.2.0"
    id("java")
//    id("com.github.sherter.google-java-format") version "0.8" apply true
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
    implementation("io.opentelemetry:opentelemetry-exporter-otlp-metrics:${otelAlphaVersion}")
    implementation(platform("io.grpc:grpc-bom:1.34.1"))
    implementation("io.grpc:grpc-netty-shaded")
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