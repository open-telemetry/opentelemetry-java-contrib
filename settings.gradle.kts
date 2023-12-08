pluginManagement {
  plugins {
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("com.gradle.enterprise") version "3.16"
    id("io.github.gradle-nexus.publish-plugin") version "1.3.0"
  }
}

plugins {
  id("com.gradle.enterprise")
}

dependencyResolutionManagement {
  repositories {
    mavenCentral()
  }
}

val gradleEnterpriseServer = "https://ge.opentelemetry.io"
val isCI = System.getenv("CI") != null
val geAccessKey = System.getenv("GRADLE_ENTERPRISE_ACCESS_KEY") ?: ""

// if GE access key is not given and we are in CI, then we publish to scans.gradle.com
val useScansGradleCom = isCI && geAccessKey.isEmpty()

if (useScansGradleCom) {
  gradleEnterprise {
    buildScan {
      termsOfServiceUrl = "https://gradle.com/terms-of-service"
      termsOfServiceAgree = "yes"
      isUploadInBackground = !isCI
      publishAlways()

      capture {
        isTaskInputFiles = true
      }
    }
  }
} else {
  gradleEnterprise {
    server = gradleEnterpriseServer
    buildScan {
      isUploadInBackground = !isCI

      this as com.gradle.enterprise.gradleplugin.internal.extension.BuildScanExtensionWithHiddenFeatures
      publishIfAuthenticated()
      publishAlways()

      capture {
        isTaskInputFiles = true
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
