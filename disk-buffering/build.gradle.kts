import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import ru.vyarus.gradle.plugin.animalsniffer.AnimalSniffer

plugins {
  id("otel.java-conventions")
  id("otel.publish-conventions")
  id("com.github.johnrengelman.shadow")
  id("me.champeau.jmh") version "0.7.2"
  id("ru.vyarus.animalsniffer") version "1.7.1"
  id("com.squareup.wire") version "4.9.7"
}

description = "Exporter implementations that store signals on disk"
otelJava.moduleName.set("io.opentelemetry.contrib.exporters.disk")

java {
  sourceCompatibility = JavaVersion.VERSION_1_8
  targetCompatibility = JavaVersion.VERSION_1_8
}

dependencies {
  api("io.opentelemetry:opentelemetry-sdk")
  compileOnly("com.google.auto.value:auto-value-annotations")
  annotationProcessor("com.google.auto.value:auto-value")
  signature("com.toasttab.android:gummy-bears-api-21:0.6.1:coreLib@signature")
  testImplementation("org.mockito:mockito-inline")
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

wire {
  java {}

  sourcePath {
    srcJar("io.opentelemetry.proto:opentelemetry-proto:1.1.0-alpha")
  }

  root(
    "opentelemetry.proto.trace.v1.TracesData",
    "opentelemetry.proto.metrics.v1.MetricsData",
    "opentelemetry.proto.logs.v1.LogsData",
  )
}

tasks.named<ShadowJar>("shadowJar") {
  archiveClassifier.set("")
  configurations = emptyList() // To avoid embedding any dependencies as we only need to rename some local packages.
  relocate("io.opentelemetry.proto", "io.opentelemetry.diskbuffering.proto")
  mustRunAfter("jar")
}

// The javadoc from wire's generated classes has errors that make the task that generates the "javadoc" artifact to fail. This
// makes the javadoc task to ignore those generated classes.
tasks.withType(Javadoc::class.java) {
  exclude("io/opentelemetry/proto/*")
}

// The task that generates the "sources" artifact fails due to a "duplicated io/opentelemetry/proto/metrics/v1/Exemplar.java" file
// Which is strange since there's only one file like that which is generated by wire and the main "jar" task doesn't raise the same issue.
// This allows to ignore any subsequent files with the same path when creating the "sources" artifact.
tasks.named("sourcesJar", Jar::class.java) {
  duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
