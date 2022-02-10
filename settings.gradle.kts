pluginManagement {
  plugins {
    id("com.github.ben-manes.versions") version "0.42.0"
    id("com.github.johnrengelman.shadow") version "7.1.2"
    id("com.gradle.enterprise") version "3.8.1"
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
include(":dependencyManagement")
include(":example")
include(":jfr-streaming")
include(":jmx-metrics")
include(":maven-extension")
include(":runtime-attach")
include(":samplers")
include(":static-instrumenter:agent-instrumenter")
include(":static-instrumenter:gradle-plugin")
include(":static-instrumenter:maven-plugin")
