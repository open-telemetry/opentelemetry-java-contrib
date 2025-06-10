# Contributing

Welcome to the OpenTelemetry Java Contrib Repository!

## Introduction

This repository focuses on providing tools and utilities for Java-based observability, such as remote JMX metric gathering and reporting. We’re excited to have you here! Whether you’re fixing a bug, adding a feature, or suggesting an idea, your contributions are invaluable.

Before submitting new features or changes to current functionality, it is recommended to first
[open an issue](https://github.com/open-telemetry/opentelemetry-java-contrib/issues/new)
and discuss your ideas or propose the changes you wish to make.

Questions? Ask in the OpenTelemetry [java channel](https://cloud-native.slack.com/archives/C014L2KCTE3)

Pull requests for bug fixes are always welcome!

## Pre-requisites

To work with this repository, ensure you have:

### Tools:

Java 17 or higher

### Platform Notes:

macOS/Linux: Ensure JAVA_HOME is set correctly.

## Workflow

1. Fork the repository
2. Clone locally
3. Create a branch before working on an issue

## Local Run/Build

In order to build and test this whole repository you need JDK 11+.

#### Snapshot builds

For developers testing code changes before a release is complete, there are
snapshot builds of the `main` branch. They are available from
the Sonatype snapshot repository at `https://central.sonatype.com/repository/maven-snapshots/`
([browse](https://central.sonatype.com/service/rest/repository/browse/maven-snapshots/io/opentelemetry/contrib/)).

#### Building from source

Building using Java 11+:

```bash
$ java -version
```

```bash
$ ./gradlew assemble
```

## Testing

```bash
$ ./gradlew test
```

### Some modules have integration tests

```
$ ./gradlew integrationTest
```

Follow the Java Instrumentation [Style Guide](https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/main/docs/contributing/style-guideline.md) from the opentelemetry-java-instrumentation repository.

Failure? Check logs for errors or mismatched dependencies.

## Gradle conventions

- Use kotlin instead of groovy
- Plugin versions should be specified in `settings.gradle.kts`, not in individual modules
- All modules use `plugins { id("otel.java-conventions") }`

## Further Help

Join [#otel-java](https://cloud-native.slack.com/archives/C014L2KCTE3) on OpenTelemetry Slack
