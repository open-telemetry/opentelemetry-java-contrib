plugins {
  application
  id("com.github.johnrengelman.shadow")
  id("otel.java-conventions")
  id("otel.publish-conventions")
}

description = "IBM-MQ metrics"
otelJava.moduleName.set("io.opentelemetry.contrib.jmxscraper")
application.mainClass.set("io.opentelemetry.contrib.jmxscraper.JmxScraper")

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
  api("org.jetbrains:annotations:26.0.2")
  api("com.ibm.mq:com.ibm.mq.allclient:9.4.2.1")
  api("org.yaml:snakeyaml:2.4")
  api("com.fasterxml.jackson.core:jackson-databind:2.19.0")
  api("io.opentelemetry:opentelemetry-sdk")
  api("io.opentelemetry:opentelemetry-exporter-otlp")
  api("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")
  api("org.slf4j:slf4j-api:2.0.7")
//  api(libs.org.apache.logging.log4j.log4j.api)
//  api(libs.org.apache.logging.log4j.log4j.core)
//  api(libs.org.apache.logging.log4j.log4j.slf4j2.impl)
//  api(libs.org.json.json)
//  testImplementation(libs.org.junit.jupiter.junit.jupiter.api)
//  testImplementation(libs.org.junit.jupiter.junit.jupiter.params)
//  testImplementation(libs.org.mockito.mockito.core)
//  testImplementation(libs.org.mockito.mockito.junit.jupiter)
//  testImplementation(libs.org.assertj.assertj.core)
//  testImplementation(libs.io.opentelemetry.opentelemetry.sdk.testing)
//  testImplementation(libs.com.ibm.mq.com.ibm.mq.jakarta.client)
//  testImplementation(libs.jakarta.jms.jakarta.jms.api)
//  testImplementation(libs.org.junit.jupiter.junit.jupiter.engine)
//  testRuntimeOnly(libs.org.junit.platform.junit.platform.launcher)
//  integrationTestImplementation(libs.org.assertj.assertj.core)
//  integrationTestImplementation(libs.org.junit.jupiter.junit.jupiter.api)
//  integrationTestImplementation(libs.io.opentelemetry.opentelemetry.sdk.testing)
//  integrationTestImplementation(libs.com.ibm.mq.com.ibm.mq.jakarta.client)
//  integrationTestImplementation(libs.jakarta.jms.jakarta.jms.api)
//  integrationTestImplementation(libs.org.junit.jupiter.junit.jupiter.engine)
//  integrationTestRuntimeOnly(libs.org.junit.platform.junit.platform.launcher)
  ibmClientJar("com.ibm.mq:com.ibm.mq.allclient:9.4.2.1") {
    artifact {
      name = "com.ibm.mq.allclient"
      extension = "jar"
    }
    isTransitive = false
  }
}

tasks.shadowJar {
  dependencies {
    exclude(dependency("com.ibm.mq:com.ibm.mq.allclient:9.4.2.1"))
  }
}
