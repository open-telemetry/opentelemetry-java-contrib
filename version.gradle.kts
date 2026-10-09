val stableVersion = "1.62.0-SNAPSHOT"
val alphaVersion = "1.62.0-alpha-SNAPSHOT"
val apidiffBaselineVersion = "1.61.0"
val tagVersion by extra { "v$stableVersion" }

allprojects {
  if (findProperty("otel.stable") != "true") {
    version = alphaVersion
  } else {
    version = stableVersion
  }
  extra["apidiffBaselineVersion"] = apidiffBaselineVersion
}
