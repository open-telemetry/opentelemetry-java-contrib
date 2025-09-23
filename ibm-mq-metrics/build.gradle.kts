plugins {
  application
  id("com.gradleup.shadow")
  id("otel.java-conventions")
  id("otel.publish-conventions")
}

description = "IBM-MQ metrics"
otelJava.moduleName.set("io.opentelemetry.contrib.ibm-mq-metrics")
application.mainClass.set("io.opentelemetry.ibm.mq.opentelemetry.Main")

sourceSets {
  create("integrationTest") {
    compileClasspath += sourceSets.main.get().output
    runtimeClasspath += sourceSets.main.get().output
  }
}

val integrationTestImplementation by configurations.getting {
  extendsFrom(configurations.implementation.get())
}
val integrationTestRuntimeOnly by configurations.getting

configurations["integrationTestRuntimeOnly"].extendsFrom(configurations.runtimeOnly.get())

val ibmClientJar: Configuration by configurations.creating {
  isCanBeResolved = true
  isCanBeConsumed = false
}

dependencies {
  api("com.google.code.findbugs:jsr305:3.0.2")
  api("io.swagger:swagger-annotations:1.6.16")
  api("org.jetbrains:annotations:26.0.2-1")
  api("com.ibm.mq:com.ibm.mq.allclient:9.4.3.1")
  api("org.yaml:snakeyaml:2.5")
  api("com.fasterxml.jackson.core:jackson-databind:2.20.0")
  api("io.opentelemetry:opentelemetry-sdk")
  api("io.opentelemetry:opentelemetry-exporter-otlp")
  api("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")
  api("org.slf4j:slf4j-api:2.0.17")
  implementation("org.slf4j:slf4j-simple:2.0.17")
  testImplementation("com.google.guava:guava")
  testImplementation("io.opentelemetry:opentelemetry-sdk-testing")
  integrationTestImplementation("org.assertj:assertj-core:3.27.6")
  integrationTestImplementation("org.junit.jupiter:junit-jupiter-api:5.13.4")
  integrationTestImplementation("io.opentelemetry:opentelemetry-sdk-testing")
  integrationTestImplementation("com.ibm.mq:com.ibm.mq.jakarta.client:9.4.3.1")
  integrationTestImplementation("jakarta.jms:jakarta.jms-api:3.1.0")
  integrationTestImplementation("org.junit.jupiter:junit-jupiter-engine:5.13.4")
  integrationTestRuntimeOnly("org.junit.platform:junit-platform-launcher:1.13.4")
  ibmClientJar("com.ibm.mq:com.ibm.mq.allclient:9.4.3.1") {
    artifact {
      name = "com.ibm.mq.allclient"
      extension = "jar"
    }
    isTransitive = false
  }
}

tasks.shadowJar {
  dependencies {
    exclude(dependency("com.ibm.mq:com.ibm.mq.allclient:9.4.3.1"))
  }
}

val integrationTest = tasks.register<Test>("integrationTest") {
  description = "Runs integration tests."
  group = "verification"

  testClassesDirs = sourceSets["integrationTest"].output.classesDirs
  classpath = sourceSets["integrationTest"].runtimeClasspath
  shouldRunAfter("test")

  useJUnitPlatform()

  testLogging {
    events("passed")
  }
}
