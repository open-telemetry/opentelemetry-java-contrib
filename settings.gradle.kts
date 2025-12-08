import org.gradle.kotlin.dsl.maven

pluginManagement {
  plugins {
    id("com.gradleup.shadow") version "9.3.0"
    id("io.github.gradle-nexus.publish-plugin") version "2.0.0"
    id("com.gradle.develocity") version "4.2.2"
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
  }
}

plugins {
  id("com.gradle.develocity")
  id("org.gradle.toolchains.foojay-resolver-convention")
}

dependencyResolutionManagement {
  repositories {
    mavenCentral()
    mavenLocal()
    // for otel snapshots
    maven {
      url = uri("https://central.sonatype.com/repository/maven-snapshots/")
    }
  }
}

val develocityServer = "https://develocity.opentelemetry.io"
val isCI = System.getenv("CI") != null
val develocityAccessKey = System.getenv("DEVELOCITY_ACCESS_KEY") ?: ""

// if develocity access key is not given and we are in CI, then we publish to scans.gradle.com
val useScansGradleCom = isCI && develocityAccessKey.isEmpty()

develocity {
  if (useScansGradleCom) {
    buildScan {
      termsOfUseUrl = "https://gradle.com/help/legal-terms-of-use"
      termsOfUseAgree = "yes"
    }
  } else {
    server = develocityServer
    buildScan {
      publishing.onlyIf { it.isAuthenticated }
    }
  }

  buildScan {
    uploadInBackground = !isCI

    capture {
      fileFingerprints = true
    }

    buildScanPublished {
      File("build-scan.txt").printWriter().use { writer ->
        writer.println(buildScanUri)
      }
    }
  }
}

if (!useScansGradleCom) {
  buildCache {
    remote(develocity.buildCache) {
      isPush = isCI && develocityAccessKey.isNotEmpty()
    }
  }
}

rootProject.name = "opentelemetry-java-contrib"

include(":aws-resources")
include(":aws-xray")
include(":aws-xray-propagator")
include(":azure-resources")
include(":baggage-processor")
include(":cel-sampler")
include(":compressors:compressor-zstd")
include(":cloudfoundry-resources")
include(":consistent-sampling")
include(":dependencyManagement")
include(":disk-buffering")
include(":ibm-mq-metrics")
include(":jfr-events")
include(":jfr-connection")
include(":jmx-metrics")
include(":jmx-scraper")
include(":jmx-scraper:test-app")
include(":jmx-scraper:test-webapp")
include(":maven-extension")
include(":micrometer-meter-provider")
include(":noop-api")
include(":processors")
include(":prometheus-client-bridge")
include(":resource-providers")
include(":runtime-attach:runtime-attach")
include(":runtime-attach:runtime-attach-core")
include(":samplers")
include(":kafka-exporter")
include(":gcp-resources")
include(":span-stacktrace")
include(":inferred-spans")
include(":opamp-client")
include(":gcp-auth-extension")
include(":dynamic-control")
