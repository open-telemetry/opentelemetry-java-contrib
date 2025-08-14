val stableVersion = "1.49.0-SNAPSHOT"
val alphaVersion = "1.49.0-alpha-SNAPSHOT"

allprojects {
  if (findProperty("otel.stable") != "true") {
    version = alphaVersion
  } else {
    version = stableVersion
  }
}
