plugins {
    `java-library`

    id("otel.spotless-conventions")
}

group = "io.opentelemetry.contrib"
version = "1.0.0-alpha"

base.archivesBaseName = "${rootProject.name}-${project.name}"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }

    withJavadocJar()
    withSourcesJar()
}

tasks {
    withType<JavaCompile>().configureEach {
        with(options) {
            release.set(8)
        }
    }

    val integrationTest by registering {
        dependsOn(test)
    }

    test {
        if (gradle.startParameter.taskNames.contains(integrationTest.name)) {
            systemProperty("ojc.integration.tests", "true")
        }
    }
}

val dependencyManagement by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = false
    isVisible = false
}

dependencies {
    dependencyManagement(platform(project(":dependencyManagement")))
    afterEvaluate {
        configurations.configureEach {
            if (isCanBeResolved && !isCanBeConsumed) {
                extendsFrom(dependencyManagement)
            }
        }
    }
}
