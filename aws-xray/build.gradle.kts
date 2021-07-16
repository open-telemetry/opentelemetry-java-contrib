plugins {
    id("otel.java-conventions")
}

description = "OpenTelemetry AWS X-Ray Support"

dependencies {
    api("io.opentelemetry:opentelemetry-api")
    api("io.opentelemetry:opentelemetry-sdk-trace")

    implementation("io.opentelemetry:opentelemetry-semconv")

    compileOnly("org.checkerframework:checker-qual")

    annotationProcessor("com.google.auto.value:auto-value")
    compileOnly("com.google.auto.value:auto-value-annotations")

    implementation("com.fasterxml.jackson.core:jackson-core")
    implementation("com.fasterxml.jackson.core:jackson-databind")

    testImplementation("com.linecorp.armeria:armeria-junit5")
    testImplementation("io.opentelemetry:opentelemetry-sdk-testing")
    testImplementation("com.google.guava:guava")
    testImplementation("org.slf4j:slf4j-simple")
    testImplementation("org.skyscreamer:jsonassert")
}
