# Contributing

🎉 Welcome to the OpenTelemetry Java Contrib Repository! 🎉

## Introduction

This repository focuses on providing tools and utilities for Java-based observability, such as remote JMX metric gathering and reporting. We’re excited to have you here! Whether you’re fixing a bug, adding a feature, or suggesting an idea, your contributions are invaluable.

-Before submitting new features or changes to current functionality, it is recommended to first
[open an issue](https://github.com/open-telemetry/opentelemetry-java-contrib/issues/new)
and discuss your ideas or propose the changes you wish to make.

-Questions? Ask in the OpenTelemetry [java channel](https://cloud-native.slack.com/archives/C014L2KCTE3)

Pull requests for bug fixes are always welcome!

## Pre-requisites

To work with this repository, ensure you have:

Tools:

-JDK 11+

-Java 17 or higher

-Gradle 7+

-Git

### Platform Notes:

macOS/Linux: Ensure JAVA_HOME is set correctly.

Windows: Use WSL2 or ensure Gradle is installed and accessible.

## Workflow

1. Fork the repository
2. Clone locally
3. Create a branch before working on an issue


## Local Run/Build

In order to build and test this whole repository you need JDK 11+.

#### Snapshot builds

For developers testing code changes before a release is complete, there are
snapshot builds of the `main` branch. They are available from
the Sonatype OSS snapshots repository at `https://oss.sonatype.org/content/repositories/snapshots/`
([browse](https://oss.sonatype.org/content/repositories/snapshots/io/opentelemetry/contrib/))

#### Building from source

Building using Java 11+:

```bash
$ java -version
```

```bash
$ ./gradlew assemble

# Build the complete project
$ ./gradlew build
```

## Testing

```bash
# Run integration tests
$ ./gradlew integrationTest
```

### Test Types:

- Unit Tests: Isolated component tests.

- Integration Tests: Validate interactions between modules.

- CI Checks: PRs must pass all tests and linting.

Failure? Check logs for errors or mismatched dependencies.


## Contributing Rules

Style Guide: Follow the Java Instrumentation Style Guide.

### Gradle conventions

- Use kotlin instead of groovy
- Plugin versions should be specified in `settings.gradle.kts`, not in individual modules
- All modules use `plugins { id("otel.java-conventions") }`

## Further Help
Join [#otel-java](https://cloud-native.slack.com/archives/C014L2KCTE3) on OpenTelemetry Slack


## Troubleshooting Guide

# Troubleshooting Guide

| Error                 | Solution |
|-----------------------|----------|
| `JAVA_HOME` not set  | Set `JAVA_HOME` to your JDK 11+ installation path. |
| `Gradle build fails` | Ensure plugin versions are specified in `settings.gradle.kts`. |
| `Test failures`      | Check for missing dependencies or version conflicts. |

