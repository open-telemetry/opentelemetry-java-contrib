plugins {
  `kotlin-dsl`
  // When updating, update below in dependencies too
  id("com.diffplug.spotless") version "7.2.1"
}

repositories {
  mavenCentral()
  gradlePluginPortal()
  mavenLocal()
}

dependencies {
  // When updating, update above in plugins too
  implementation("com.diffplug.spotless:spotless-plugin-gradle:7.2.1")
  implementation("net.ltgt.gradle:gradle-errorprone-plugin:4.3.0")
  implementation("net.ltgt.gradle:gradle-nullaway-plugin:2.3.0")
  implementation("org.owasp:dependency-check-gradle:12.1.3")
  implementation("ru.vyarus.animalsniffer:ru.vyarus.animalsniffer.gradle.plugin:2.0.1")
  implementation("com.gradle:develocity-gradle-plugin:4.1.1")
  implementation("me.champeau.gradle:japicmp-gradle-plugin:0.4.6")
  implementation("com.google.auto.value:auto-value-annotations:1.11.0")
}

spotless {
  kotlinGradle {
    ktlint().editorConfigOverride(mapOf(
      "indent_size" to "2",
      "continuation_indent_size" to "2",
      "max_line_length" to "160",
      "insert_final_newline" to "true",
      "ktlint_standard_no-wildcard-imports" to "disabled",
      // ktlint does not break up long lines, it just fails on them
      "ktlint_standard_max-line-length" to "disabled",
      // ktlint makes it *very* hard to locate where this actually happened
      "ktlint_standard_trailing-comma-on-call-site" to "disabled",
      // depends on ktlint_standard_wrapping
      "ktlint_standard_trailing-comma-on-declaration-site" to "disabled",
      // also very hard to find out where this happens
      "ktlint_standard_wrapping" to "disabled"
    ))
    target("**/*.gradle.kts")
  }
}
