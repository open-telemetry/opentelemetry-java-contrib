val stableVersion = "1.57.0"
val alphaVersion = "1.57.0-alpha"
val tagVersion by extra { "v$stableVersion" }

allprojects {
  if (findProperty("otel.stable") != "true") {
    version = alphaVersion
  } else {
    version = stableVersion
  }
}
