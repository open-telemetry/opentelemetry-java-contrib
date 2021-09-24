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

tasks {
    withType<GroovyCompile> {
        sourceCompatibility = "8"
        targetCompatibility = "8"
    }
}
