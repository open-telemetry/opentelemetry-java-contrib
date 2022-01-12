pluginManagement {
  plugins {
    id("com.github.ben-manes.versions") version "0.39.0"
    id("com.github.johnrengelman.shadow") version "7.1.1"
    id("com.gradle.enterprise") version "3.8"
    id("org.unbroken-dome.test-sets") version "4.0.0"
    id("io.github.gradle-nexus.publish-plugin") version "1.1.0"
    id("nebula.release") version "16.0.0"
  }
}

plugins {
  id("com.gradle.enterprise")
}

dependencyResolutionManagement {
  repositories {
    mavenCentral()
    mavenLocal()
  }
}

val isCI = System.getenv("CI") != null
val skipBuildscan = System.getenv("SKIP_BUILDSCAN").toBoolean()
gradleEnterprise {
  buildScan {
    termsOfServiceUrl = "https://gradle.com/terms-of-service"
    termsOfServiceAgree = "yes"

    if (isCI && !skipBuildscan) {
      publishAlways()
      tag("CI")
    }
  }
}

rootProject.name = "opentelemetry-java-contrib"

include(":all")
include(":aws-xray")
include(":samplers")
include(":dependencyManagement")
include(":disruptor-processor")
include(":example")
include(":jfr-streaming")
include(":jmx-metrics")
include(":maven-extension")
include(":runtime-attach")
