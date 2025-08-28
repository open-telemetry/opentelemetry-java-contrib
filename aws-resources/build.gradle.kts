plugins {
  id("otel.java-conventions")

  id("otel.publish-conventions")
}

description = "OpenTelemetry AWS Resources Support"
otelJava.moduleName.set("io.opentelemetry.contrib.aws.resource")

dependencies {
  api("io.opentelemetry:opentelemetry-api")
  compileOnly("io.opentelemetry:opentelemetry-api-incubator")
  api("io.opentelemetry:opentelemetry-sdk")

  implementation("io.opentelemetry.semconv:opentelemetry-semconv")
  testImplementation("io.opentelemetry.semconv:opentelemetry-semconv-incubating")

  compileOnly("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")

  implementation("com.fasterxml.jackson.core:jackson-core")
  implementation("com.squareup.okhttp3:okhttp")

  testImplementation("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")
  testImplementation("io.opentelemetry:opentelemetry-sdk-extension-incubator")
  testImplementation("io.opentelemetry:opentelemetry-api-incubator")
  testImplementation("io.opentelemetry:opentelemetry-sdk-testing")
  testImplementation("io.opentelemetry:opentelemetry-exporter-logging")

  testImplementation("com.linecorp.armeria:armeria-junit5")
  testRuntimeOnly("org.bouncycastle:bcpkix-jdk15on")
  testImplementation("com.google.guava:guava")
  testImplementation("org.skyscreamer:jsonassert")
  testImplementation("org.junit-pioneer:junit-pioneer")
}

tasks {
  withType<Test>().configureEach {
    environment(
      "AWS_REGION" to "us-east-1",
      "AWS_LAMBDA_FUNCTION_NAME" to "my-function",
      "AWS_LAMBDA_FUNCTION_VERSION" to "1.2.3"
    )
    jvmArgs("-Dotel.experimental.config.file=${project.projectDir.resolve("src/test/resources/config.yaml")}")
  }
}
