pluginManagement {
    plugins {
        id("com.github.ben-manes.versions") version "0.39.0"
        id("com.github.johnrengelman.shadow") version "7.0.0"
        id("org.unbroken-dome.test-sets") version "4.0.0"
        id("io.github.gradle-nexus.publish-plugin") version "1.1.0"
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        mavenLocal()
    }
}

rootProject.name = "opentelemetry-java-contrib"

include(":all")
include(":aws-xray")
include(":dependencyManagement")
include(":example")
include(":jmx-metrics")
