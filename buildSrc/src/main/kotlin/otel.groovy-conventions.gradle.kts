plugins {
    groovy

    id("otel.java-conventions")
    id("otel.spotless-conventions")
}

dependencies {
    testImplementation("org.spockframework:spock-core:1.3-groovy-2.5") {
        exclude("org.codehaus.groovy", "groovy-all")
    }
}

spotless {
    groovy {
        greclipse()
        indentWithSpaces()
        trimTrailingWhitespace()
        endWithNewline()
        excludeJava()
        licenseHeaderFile(rootProject.file("config/license/spotless.license.java"), "(package|import|class|def|// Includes work from:)")
    }

    groovyGradle {
        greclipse()
        indentWithSpaces()
        trimTrailingWhitespace()
        endWithNewline()
    }
}

tasks {
    withType<GroovyCompile> {
        sourceCompatibility = "8"
        targetCompatibility = "8"
    }
}
