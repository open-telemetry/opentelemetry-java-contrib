val stableVersion = "1.60.0"
val alphaVersion = "1.60.0-alpha"
val apidiffBaselineVersion = "1.59.0"
val tagVersion by extra { "v$stableVersion" }

allprojects {
  if (findProperty("otel.stable") != "true") {
    version = alphaVersion
  } else {
    version = stableVersion
  }
  extra["apidiffBaselineVersion"] = apidiffBaselineVersion
}
