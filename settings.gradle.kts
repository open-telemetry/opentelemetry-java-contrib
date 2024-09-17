pluginManagement {
  plugins {
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("com.gradle.develocity") version "3.18.1"
    id("io.github.gradle-nexus.publish-plugin") version "2.0.0"
  }
}

plugins {
  id("com.gradle.develocity")
}

dependencyResolutionManagement {
  repositories {
    mavenCentral()
    mavenLocal()
  }
}

val gradleEnterpriseServer = "https://ge.opentelemetry.io"
val isCI = System.getenv("CI") != null
val develocityAccessKey = System.getenv("GRADLE_ENTERPRISE_ACCESS_KEY") ?: ""

// if GE access key is not given and we are in CI, then we publish to scans.gradle.com
val useScansGradleCom = isCI && develocityAccessKey.isEmpty()

if (useScansGradleCom) {
  develocity {
    buildScan {
      termsOfUseUrl.set("https://gradle.com/terms-of-service")
      termsOfUseAgree.set("yes")
      uploadInBackground.set(!isCI)
      publishing.onlyIf { true }

      capture {
        fileFingerprints.set(true)
      }
    }
  }
} else {
  develocity {
    server = gradleEnterpriseServer
    buildScan {
      uploadInBackground.set(!isCI)

      publishing.onlyIf {
        it.isAuthenticated
      }
      capture {
        fileFingerprints.set(true)
      }

      gradle.startParameter.projectProperties["testJavaVersion"]?.let { tag(it) }
      gradle.startParameter.projectProperties["testJavaVM"]?.let { tag(it) }
    }
  }
}

rootProject.name = "opentelemetry-java-contrib"

include(":all")
include(":aws-resources")
include(":aws-xray")
include(":aws-xray-propagator")
include(":baggage-processor")
include(":compressors:compressor-zstd")
include(":consistent-sampling")
include(":dependencyManagement")
include(":disk-buffering")
include(":example")
include(":jfr-events")
include(":jfr-connection")
include(":jmx-metrics")
include(":jmx-scraper")
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
include(":span-stacktrace")
include(":inferred-spans")
