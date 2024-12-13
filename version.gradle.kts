val stableVersion = "1.43.0-SNAPSHOT"
val alphaVersion = "1.43.0-alpha-SNAPSHOT"

allprojects {
  if (findProperty("otel.stable") != "true") {
    version = alphaVersion
  } else {
    version = stableVersion
  }
}
