subprojects {
  // https://github.com/gradle/gradle/issues/847
  group = "io.opentelemetry.compressors"
  val proj = this
  plugins.withId("java") {
    configure<BasePluginExtension> {
      archivesName.set("opentelemetry-compressor-${proj.name}")
    }
  }
}
