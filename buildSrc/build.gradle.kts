plugins {
  `kotlin-dsl`
  // When updating, update below in dependencies too
  id("com.diffplug.spotless") version "6.9.0"
}

repositories {
  mavenCentral()
  gradlePluginPortal()
  mavenLocal()
}

dependencies {
  // When updating, update above in plugins too
  implementation("com.diffplug.spotless:spotless-plugin-gradle:6.9.0")
  implementation("net.ltgt.gradle:gradle-errorprone-plugin:2.0.2")
  implementation("net.ltgt.gradle:gradle-nullaway-plugin:1.4.0")
}

spotless {
  kotlinGradle {
    ktlint().editorConfigOverride(mapOf("indent_size" to "2", "continuation_indent_size" to "2", "disabled_rules" to "no-wildcard-imports"))
    target("**/*.gradle.kts")
  }
}
