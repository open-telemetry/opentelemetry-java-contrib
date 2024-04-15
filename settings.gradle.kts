pluginManagement {
  plugins {
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("io.github.gradle-nexus.publish-plugin") version "2.0.0"
  }
}

dependencyResolutionManagement {
  repositories {
    mavenCentral()
  }
}

rootProject.name = "opentelemetry-java-contrib"

include(":all")
include(":aws-resources")
include(":aws-xray")
include(":aws-xray-propagator")
include(":compressors:compressor-zstd")
include(":consistent-sampling")
include(":dependencyManagement")
include(":disk-buffering")
include(":example")
include(":jfr-events")
include(":jfr-connection")
include(":jmx-metrics")
include(":maven-extension")
include(":micrometer-meter-provider")
include(":noop-api")
include(":processors")
include(":prometheus-client-bridge")
include(":resource-providers")
include(":runtime-attach:runtime-attach")
include(":runtime-attach:runtime-attach-core")
include(":samplers")
include(":static-instrumenter:agent-instrumenter")
include(":static-instrumenter:maven-plugin")
include(":static-instrumenter:agent-extension")
include(":static-instrumenter:bootstrap")
include(":static-instrumenter:test-app")
include(":kafka-exporter")
include(":gcp-resources")
