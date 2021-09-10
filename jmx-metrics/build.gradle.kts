plugins {
    application
    id("com.github.johnrengelman.shadow")

    id("otel.groovy-conventions")
    id("otel.publish-conventions")
}

description = "JMX metrics gathering Groovy script runner"

application.mainClass.set("io.opentelemetry.contrib.jmxmetrics.JmxMetrics")

repositories {
    mavenCentral()
    maven {
        setUrl("https://repo.terracotta.org/maven2")
        content {
            includeGroupByRegex("""org\.terracotta.*""")
        }
    }
    mavenLocal()
}

val groovyVersion = "2.5.11"

dependencies {
    api(platform("org.codehaus.groovy:groovy-bom:${groovyVersion}"))

    implementation("io.grpc:grpc-netty-shaded")
    implementation("org.codehaus.groovy:groovy-jmx")
    implementation("org.codehaus.groovy:groovy")
    implementation("io.prometheus:simpleclient")
    implementation("io.prometheus:simpleclient_httpserver")
    implementation("io.opentelemetry:opentelemetry-api")
    implementation("io.opentelemetry:opentelemetry-api-metrics")
    implementation("io.opentelemetry:opentelemetry-sdk")
    implementation("io.opentelemetry:opentelemetry-sdk-metrics")
    implementation("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")
    implementation("io.opentelemetry:opentelemetry-sdk-testing")
    implementation("io.opentelemetry:opentelemetry-exporter-logging")
    implementation("io.opentelemetry:opentelemetry-exporter-otlp-metrics")
    implementation("io.opentelemetry:opentelemetry-exporter-prometheus")
    implementation("org.slf4j:slf4j-api")
    implementation("org.slf4j:slf4j-simple")

    runtimeOnly("org.terracotta:jmxremote_optional-tc:1.0.8")

    testImplementation("io.grpc:grpc-api")
    testImplementation("io.grpc:grpc-protobuf")
    testImplementation("io.grpc:grpc-stub")
    testImplementation("io.grpc:grpc-testing")
    testImplementation("org.codehaus.groovy:groovy-test")
    testImplementation("io.rest-assured:rest-assured:4.2.0")
    testImplementation("org.awaitility:awaitility")
    testImplementation("org.apache.httpcomponents.client5:httpclient5-fluent:5.0.1")
    testImplementation("org.testcontainers:testcontainers")
    testImplementation("io.opentelemetry:opentelemetry-proto")
}

tasks {
    shadowJar {
        manifest {
            attributes["Implementation-Version"] = project.version
        }
        // This should always be standalone, so remove "-all" to prevent unnecessary artifact.
        archiveClassifier.set("")
    }

    withType<Test>().configureEach {
        dependsOn(shadowJar)
        systemProperty("shadow.jar.path", shadowJar.get().archiveFile.get().asFile.absolutePath)
        systemProperty("gradle.project.version", "${project.version}")
    }
}
