import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    `java-library`

    id("otel.spotless-conventions")
}

group = "io.opentelemetry.contrib"

base.archivesName.set("opentelemetry-${project.name}")

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

    withType<Test>().configureEach {
        useJUnitPlatform()

        testLogging {
            exceptionFormat = TestExceptionFormat.FULL
            showExceptions = true
            showCauses = true
            showStackTraces = true
        }
    }

    withType<Javadoc>().configureEach {
        exclude("io/opentelemetry/**/internal/**")

        with(options as StandardJavadocDocletOptions) {
            source = "8"
            encoding = "UTF-8"
            docEncoding = "UTF-8"
            breakIterator(true)

            addBooleanOption("html5", true)

            links("https://docs.oracle.com/javase/8/docs/api/")
            addBooleanOption("Xdoclint:all,-missing", true)
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

    testImplementation("org.assertj:assertj-core")
    testImplementation("org.awaitility:awaitility")
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.junit.jupiter:junit-jupiter-params")

    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine")
}
