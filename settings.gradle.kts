pluginManagement {
  plugins {
    id("com.gradleup.shadow") version "9.0.1"
    id("io.github.gradle-nexus.publish-plugin") version "2.0.0"
    id("com.gradle.develocity") version "4.1"
  }
}

plugins {
  id("com.gradle.develocity")
}

dependencyResolutionManagement {
  repositories {
    mavenCentral()
    mavenLocal()
    // terracotta repository for jmxmp connector
    maven {
      setUrl("https://repo.terracotta.org/maven2")
      content {
        includeGroupByRegex("""org\.terracotta.*""")
      }
    }
  }
}

develocity {
  buildScan {
    publishing.onlyIf { System.getenv("CI") != null }
    termsOfUseUrl.set("https://gradle.com/help/legal-terms-of-use")
    termsOfUseAgree.set("yes")
  }
}

rootProject.name = "opentelemetry-java-contrib"

include(":all")
include(":aws-resources")
include(":aws-xray")
include(":aws-xray-propagator")
include(":azure-resources")
include(":baggage-processor")
include(":compressors:compressor-zstd")
include(":cloudfoundry-resources")
include(":consistent-sampling")
include(":dependencyManagement")
include(":disk-buffering")
include(":example")
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
