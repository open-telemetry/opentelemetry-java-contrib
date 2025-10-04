plugins {
  id("otel.java-conventions")

  id("otel.publish-conventions")
}

description = "OpenTelemetry CloudFoundry Resources"
otelJava.moduleName.set("io.opentelemetry.contrib.cloudfoundry.resources")

dependencies {
  api("io.opentelemetry:opentelemetry-api")
  compileOnly("io.opentelemetry:opentelemetry-api-incubator")
  api("io.opentelemetry:opentelemetry-sdk")
  compileOnly("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure-spi")

  compileOnly("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")

  implementation("com.fasterxml.jackson.core:jackson-core")
  implementation("io.opentelemetry.semconv:opentelemetry-semconv")
  testImplementation("io.opentelemetry.semconv:opentelemetry-semconv-incubating")

  testImplementation("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")
  testImplementation("io.opentelemetry:opentelemetry-api-incubator")
  testImplementation("io.opentelemetry:opentelemetry-sdk-testing")
  testImplementation("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure-spi")
  testImplementation("io.opentelemetry:opentelemetry-exporter-logging")
  testImplementation("io.opentelemetry:opentelemetry-sdk-extension-incubator")
}

tasks {
  withType<Test>().configureEach {
    environment(
      "VCAP_APPLICATION" to """
        {
          "application_id": "0193a038-e615-7e5e-92ca-f4bcd7ba0a25",
          "application_name": "cf-app-name",
          "application_uris": [
            "testapp.example.com"
          ],
          "cf_api": "https://api.cf.example.com",
          "limits": {
            "fds": 256
          },
          "instance_index": 1,
          "organization_id": "0193a375-8d8e-7e0c-a832-01ce9ded40dc",
          "organization_name": "cf-org-name",
          "process_id": "0193a4e3-8fd3-71b9-9fe3-5640c53bf1e2",
          "process_type": "web",
          "space_id": "0193a7e7-da17-7ea4-8940-b1e07b401b16",
          "space_name": "cf-space-name",
          "users": null
        }
      """.trimIndent(),
    )
    jvmArgs("-Dotel.experimental.config.file=${project.projectDir.resolve("src/test/resources/declarative-config.yaml")}")
  }
}
