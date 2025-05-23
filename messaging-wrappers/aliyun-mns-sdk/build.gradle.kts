plugins {
  id("otel.java-conventions")

  id("otel.publish-conventions")
}

description = "OpenTelemetry Messaging Wrappers - aliyun-mns-sdk implementation"
otelJava.moduleName.set("io.opentelemetry.contrib.messaging.wrappers.aliyun-mns-sdk")

dependencies {
  api(project(":messaging-wrappers:api"))

  compileOnly("com.aliyun.mns:aliyun-sdk-mns:1.3.0")

  testImplementation("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")
  testImplementation("io.opentelemetry:opentelemetry-sdk-trace")
  testImplementation("io.opentelemetry:opentelemetry-sdk-testing")

  testImplementation("io.opentelemetry:opentelemetry-sdk-extension-incubator")
  testImplementation("uk.org.webcompere:system-stubs-jupiter:2.0.3")
}
