plugins {
  id("otel.java-conventions")
  id("otel.publish-conventions")
}

description = "Contributed ResourceProviders"

dependencies {
  annotationProcessor("com.google.auto.service:auto-service")

  compileOnly("com.google.auto.service:auto-service")
  compileOnly("io.opentelemetry:opentelemetry-api")
  compileOnly("io.opentelemetry:opentelemetry-sdk")
  compileOnly("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")
  compileOnly("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure-spi")
  compileOnly("io.opentelemetry:opentelemetry-semconv")
  compileOnly("io.opentelemetry.javaagent:opentelemetry-javaagent-tooling")
//  testImplementation("org.mockito:mockito-junit-jupiter")
//  testImplementation("org.junit.jupiter:junit-jupiter-api")
//  testImplementation("org.junit.jupiter:junit-jupiter-params")
//  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
  testImplementation("com.google.auto.service:auto-service")
  testImplementation("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")
  testImplementation("io.opentelemetry.javaagent:opentelemetry-javaagent-tooling")
}
