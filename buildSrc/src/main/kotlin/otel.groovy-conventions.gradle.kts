plugins {
    groovy

    id("otel.java-conventions")
    id("otel.spotless-conventions")
}

tasks {
    withType<GroovyCompile> {
        sourceCompatibility = "8"
        targetCompatibility = "8"
    }
}
