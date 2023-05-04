pluginManagement {
  plugins {
    id("com.github.ben-manes.versions") version "0.46.0"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("com.gradle.enterprise") version "3.13.1"
    id("io.github.gradle-nexus.publish-plugin") version "1.3.0"
  }
}

plugins {
  id("com.gradle.enterprise")
}

dependencyResolutionManagement {
  repositories {
    mavenCentral()
    maven {
      url = uri("https://oss.sonatype.org/content/repositories/snapshots")
    }
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
include(":aws-resources")
include(":aws-xray")
include(":aws-xray-propagator")
include(":consistent-sampling")
include(":dependencyManagement")
include(":example")
include(":jfr-events")
include(":jfr-connection")
include(":jmx-metrics")
include(":maven-extension")
include(":micrometer-meter-provider")
include(":noop-api")
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
