plugins {
  application
  id("com.github.johnrengelman.shadow")
  id("org.unbroken-dome.test-sets")

  id("otel.groovy-conventions")
  id("otel.publish-conventions")
}

description = "JMX metrics gathering Groovy script runner"

application.mainClass.set("io.opentelemetry.contrib.jmxmetrics.JmxMetrics")

repositories {
  mavenCentral()
  maven {
    setUrl("https://repo.terracotta.org/maven2")
    content {
      includeGroupByRegex("""org\.terracotta.*""")
    }
  }
  mavenLocal()
}

val groovyVersion = "3.0.8"

testSets {
  create("integrationTest")
}

dependencies {
  api(platform("org.codehaus.groovy:groovy-bom:$groovyVersion"))

  implementation("io.grpc:grpc-netty-shaded")
  implementation("org.codehaus.groovy:groovy-jmx")
  implementation("org.codehaus.groovy:groovy")
  implementation("io.prometheus:simpleclient")
  implementation("io.prometheus:simpleclient_httpserver")
  implementation("io.opentelemetry:opentelemetry-api")
  implementation("io.opentelemetry:opentelemetry-sdk")
  implementation("io.opentelemetry:opentelemetry-sdk-metrics")
  implementation("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")
  implementation("io.opentelemetry:opentelemetry-sdk-metrics-testing")
  implementation("io.opentelemetry:opentelemetry-sdk-testing")
  implementation("io.opentelemetry:opentelemetry-exporter-logging")
  implementation("io.opentelemetry:opentelemetry-exporter-otlp-metrics")
  implementation("io.opentelemetry:opentelemetry-exporter-prometheus")
  implementation("org.slf4j:slf4j-api")
  implementation("org.slf4j:slf4j-simple")

  annotationProcessor("com.google.auto.value:auto-value")
  compileOnly("com.google.auto.value:auto-value-annotations")

  runtimeOnly("org.terracotta:jmxremote_optional-tc:1.0.8")

  testImplementation("io.grpc:grpc-api")
  testImplementation("io.grpc:grpc-protobuf")
  testImplementation("io.grpc:grpc-stub")
  testImplementation("io.grpc:grpc-testing")
  testImplementation("org.junit-pioneer:junit-pioneer")
  testImplementation("org.awaitility:awaitility")
  testImplementation("org.testcontainers:testcontainers")
  testImplementation("io.opentelemetry.proto:opentelemetry-proto:0.11.0-alpha")

  add("integrationTestImplementation", "com.linecorp.armeria:armeria-grpc")
  add("integrationTestImplementation", "com.linecorp.armeria:armeria-junit5")
  add("integrationTestImplementation", "org.testcontainers:junit-jupiter")
}

tasks {
  shadowJar {
    manifest {
      attributes["Implementation-Version"] = project.version
    }
    // This should always be standalone, so remove "-all" to prevent unnecessary artifact.
    archiveClassifier.set("")
  }

  withType<Test>().configureEach {
    dependsOn(shadowJar)
    systemProperty("shadow.jar.path", shadowJar.get().archiveFile.get().asFile.absolutePath)
    systemProperty("gradle.project.version", "${project.version}")
  }
}
