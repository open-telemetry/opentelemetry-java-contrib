val stableVersion = "1.34.0-SNAPSHOT"
val alphaVersion = "1.34.0-alpha-SNAPSHOT"

allprojects {
  if (findProperty("otel.stable") != "true") {
    version = alphaVersion
  } else {
    version = stableVersion
  }
}
