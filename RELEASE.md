# Releasing OpenTelemetry Java Contrib Artifacts

This project currently has three gradle tasks capable of preparing and releasing artifacts: `mavenPublish`,
`ossSnapshot`, and `otelRelease`.  In order for you to register your contributed project to be published by
these commands, you must apply the provided publish script plugin in your subproject's gradle file:

```groovy
apply from: project.publish
```

To set the desired library name and description for your artifact's pom file, you can set the `libraryName`
project property and project description:

```groovy
ext.libraryName = 'An OpenTelemetry Java Contrib Library'
description = 'An OpenTelemetry-based library for improving the observability of your application'
```

If you are using the [shadow gradle plugin](https://plugins.gradle.org/plugin/com.github.johnrengelman.shadow)
and would like your `shadowJar` task-produced artifact published, please set the `shadowPublish` project
property to `true` in your subproject's gradle file:

```groovy
apply plugin: 'com.github.johnrengelman.shadow'
ext.shadowPublish = true
```

## `./gradlew mavenPublish`

This task will invoke the [Maven Publish](https://docs.gradle.org/current/userguide/publishing_maven.html) task
and publish all applicable artifacts to a `build/repo` directory in the root OpenTelemetry Java Contrib project path of
your machine. The ability to publish to a stable remote repository like Maven Central is not provided at this time.

## `./gradlew ossSnapshot`

This task will invoke the [Gradle Nexus Publish Plugin](https://github.com/gradle-nexus/publish-plugin)
and publish all applicable snapshot artifacts to https://oss.sonatype.org/content/repositories/snapshots.  It's important
to note that these snapshot releases are often from unstable development states and should generally not be used in
production environments.

This task requires an account and password for Sonatype's OSSRH with `io.opentelemetry` group permissions.  If you have been
provided access please set the required gradle properties detailed in the publish script plugin.

## `./gradlew otelRelease`

This task will also invoke the [Gradle Nexus Publish Plugin](https://github.com/gradle-nexus/publish-plugin)
and publish all applicable artifacts to a new staging repository at https://oss.sonatype.org/#stagingRepositories before
closing it for manual release through the Nexus UI.  Releasing the repository will initiate automatic syncing with
Maven Central.

Like `ossSnapshot`, this task requires an account and permissions for Sonatype OSSRH, in addition to a PGP key registered
with a public keyserver.
