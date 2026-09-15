import org.gradle.kotlin.dsl.maven

pluginManagement {
  plugins {
    id("com.gradleup.shadow") version "9.6.1"
    id("io.github.gradle-nexus.publish-plugin") version "2.0.0"
    id("com.gradle.develocity") version "4.5.0"
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
    if (providers.gradleProperty("useLocalMaven").isPresent) {
      mavenLocal()
    }
    // for otel snapshots
    maven {
      url = uri("https://central.sonatype.com/repository/maven-snapshots/")
    }
  }
}

val develocityServer = "https://community.develocity.cloud"
val isCI = System.getenv("CI") != null
val develocityAccessKey = System.getenv("DEVELOCITY_ACCESS_KEY") ?: ""
val isRemoteBuildCachePushEnabled = isCI && develocityAccessKey.isNotEmpty()
val shouldDisableLocalBuildCache =
  isRemoteBuildCachePushEnabled && System.getenv("GITHUB_REF_NAME") == "main"

develocity {
  if (develocityAccessKey.isNotEmpty()) {
    server = develocityServer
    projectId = "OpenTelemetry"
  }

  buildScan {
    if (develocityAccessKey.isNotEmpty()) {
    } else if (isCI) {
      termsOfUseUrl = "https://gradle.com/help/legal-terms-of-use"
      termsOfUseAgree = "yes"
    } else {
      publishing.onlyIf { false }
    }

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

buildCache {
  // A task loaded from the local build cache is never pushed to the remote build cache, so on main
  // builds that write the remote cache the local cache is disabled, otherwise everything that
  // doesn't change is served locally, never re-executed, and never reaches the remote cache.
  local {
    isEnabled = !shouldDisableLocalBuildCache
  }

  remote(develocity.buildCache) {
    server = develocityServer
    isPush = isRemoteBuildCachePushEnabled
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
include(":custom-checks")
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
