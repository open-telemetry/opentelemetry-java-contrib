import ru.vyarus.gradle.plugin.animalsniffer.AnimalSniffer

plugins {
  id("otel.java-conventions")
  id("otel.publish-conventions")
  id("me.champeau.jmh") version "0.7.1"
  id("ru.vyarus.animalsniffer") version "1.7.1"
}

description = "Exporter implementations that store signals on disk"
otelJava.moduleName.set("io.opentelemetry.contrib.exporters.disk")

java {
  sourceCompatibility = JavaVersion.VERSION_1_8
  targetCompatibility = JavaVersion.VERSION_1_8
}

val autovalueVersion = "1.10.1"
dependencies {
  api("io.opentelemetry:opentelemetry-sdk")
  implementation("io.opentelemetry:opentelemetry-exporter-otlp-common")
  implementation("io.opentelemetry.proto:opentelemetry-proto:0.20.0-alpha")
  compileOnly("com.google.auto.value:auto-value-annotations:$autovalueVersion")
  annotationProcessor("com.google.auto.value:auto-value:$autovalueVersion")
  signature("com.toasttab.android:gummy-bears-api-24:0.5.1@signature")
  testImplementation("org.mockito:mockito-inline:4.11.0")
  testImplementation("io.opentelemetry:opentelemetry-sdk-testing")
}

animalsniffer {
  sourceSets = listOf(java.sourceSets.main.get())
}

// Always having declared output makes this task properly participate in tasks up-to-date checks
tasks.withType<AnimalSniffer> {
  reports.text.required.set(true)
}

// Attaching animalsniffer check to the compilation process.
tasks.named("classes").configure {
  finalizedBy("animalsnifferMain")
}

jmh {
  warmupIterations.set(0)
  fork.set(2)
  iterations.set(5)
  timeOnIteration.set("5s")
  timeUnit.set("ms")
}
