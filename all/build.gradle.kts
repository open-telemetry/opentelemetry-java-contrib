plugins {
    id("otel.java-conventions")
}

description = "OpenTelemetry Contrib Testing"

dependencies {
    rootProject.subprojects.forEach { subproject ->
        // Generate aggregate coverage report for published modules that enable jacoco.
        subproject.plugins.withId("jacoco") {
            subproject.plugins.withId("maven-publish") {
                implementation(project(subproject.path)) {
                    isTransitive = false
                }
            }
        }
    }
}

// https://docs.gradle.org/current/samples/sample_jvm_multi_project_with_code_coverage.html

val sourcesPath by configurations.creating {
    isVisible = false
    isCanBeResolved = true
    isCanBeConsumed = false
    extendsFrom(configurations.implementation.get())
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.DOCUMENTATION))
        attribute(DocsType.DOCS_TYPE_ATTRIBUTE, objects.named("source-folders"))
    }
}

val coverageDataPath by configurations.creating {
    isVisible = false
    isCanBeResolved = true
    isCanBeConsumed = false
    extendsFrom(configurations.implementation.get())
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.DOCUMENTATION))
        attribute(DocsType.DOCS_TYPE_ATTRIBUTE, objects.named("jacoco-coverage-data"))
    }
}

tasks.named<JacocoReport>("jacocoTestReport") {
    enabled = true

    configurations.runtimeClasspath.get().forEach {
        additionalClassDirs(zipTree(it).filter {
            // Exclude mrjar (jacoco complains), shaded, and generated code
            !it.absolutePath.contains("META-INF/versions/") &&
                    !it.absolutePath.contains("AutoValue_")
        })
    }
    additionalSourceDirs(sourcesPath.incoming.artifactView { lenient(true) }.files)
    executionData(coverageDataPath.incoming.artifactView { lenient(true) }.files.filter { it.exists() })

    reports {
        // xml is usually used to integrate code coverage with
        // other tools like SonarQube, Coveralls or Codecov
        xml.required.set(true)

        // HTML reports can be used to see code coverage
        // without any external tools
        html.required.set(true)
    }
}
