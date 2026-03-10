val stableVersion = "1.55.0-SNAPSHOT"
val alphaVersion = "1.55.0-alpha-SNAPSHOT"
val tagVersion by extra { "v$stableVersion" }

allprojects {
  if (findProperty("otel.stable") != "true") {
    version = alphaVersion
  } else {
    version = stableVersion
  }
}
