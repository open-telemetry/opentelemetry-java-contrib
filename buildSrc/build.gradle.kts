plugins {
    `kotlin-dsl`
    // When updating, update below in dependencies too
    id("com.diffplug.spotless") version "5.14.0"
}

repositories {
    mavenCentral()
    gradlePluginPortal()
    mavenLocal()
}

dependencies {
    // When updating, update above in plugins too
    implementation("com.diffplug.spotless:spotless-plugin-gradle:5.14.0")
}
