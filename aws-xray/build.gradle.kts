plugins {
    id("otel.java-conventions")
    id("otel.publish-conventions")

    id("org.unbroken-dome.test-sets")
}

description = "OpenTelemetry AWS X-Ray Support"

testSets {
    create("awsTest")
}

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

    add("awsTestImplementation", "io.opentelemetry:opentelemetry-exporter-otlp-trace")
    add("awsTestImplementation", "org.testcontainers:junit-jupiter")
}
