plugins {
  id("otel.java-conventions")

  id("otel.publish-conventions")
}

description = "OpenTelemetry Messaging Wrappers - aliyun-mns-sdk implementation"
otelJava.moduleName.set("io.opentelemetry.contrib.messaging.wrappers.aliyun-mns-sdk")

dependencies {
  api(project(":messaging-wrappers:api"))

  compileOnly("com.aliyun.mns:aliyun-sdk-mns:1.3.0")

  testImplementation("com.aliyun.mns:aliyun-sdk-mns:1.3.0")
  testImplementation(project(":messaging-wrappers:testing"))

  testImplementation("org.springframework.boot:spring-boot-starter-web:2.7.18")
  testImplementation("org.springframework.boot:spring-boot-starter-test:2.7.18")
}

tasks {
  withType<Test>().configureEach {
    jvmArgs("-Dotel.java.global-autoconfigure.enabled=true")
    // TODO: According to https://opentelemetry.io/docs/specs/semconv/messaging/messaging-spans/#message-creation-context-as-parent-of-process-span,
    //  process span should be the child of receive span. However, we couldn't access the trace context with receive span
    //  in wrappers, unless we add a generic accessor for that.
    jvmArgs("-Dotel.instrumentation.messaging.experimental.receive-telemetry.enabled=false")
    jvmArgs("-Dotel.traces.exporter=logging")
    jvmArgs("-Dotel.metrics.exporter=logging")
    jvmArgs("-Dotel.logs.exporter=logging")
  }
}

configurations.testRuntimeClasspath {
  resolutionStrategy {
    force("ch.qos.logback:logback-classic:1.2.12")
    force("org.slf4j:slf4j-api:1.7.35")
  }
}
