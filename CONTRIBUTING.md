# Contributing

Welcome to the OpenTelemetry Java Contrib repository!

## Introduction

This repository provides observability libraries and utilities for Java applications that complement
the [OpenTelemetry Java SDK](https://github.com/open-telemetry/opentelemetry-java) and
[OpenTelemetry Java Instrumentation](https://github.com/open-telemetry/opentelemetry-java-instrumentation)
projects.

Before submitting new features or changes, please consider
[opening an issue](https://github.com/open-telemetry/opentelemetry-java-contrib/issues/new) first to
discuss your ideas.

Pull requests for bug fixes are always welcome!

## Building and Testing

While most modules target Java 8, building this project requires Java 17 or higher.

To build the project:

```bash
./gradlew assemble
```

To run the tests:

```bash
./gradlew test
```

Some modules include integration tests that can be run with:

```bash
./gradlew integrationTest
```

## Snapshot Builds

Snapshot builds of the `main` branch are available from the Sonatype snapshot repository at:
`https://central.sonatype.com/repository/maven-snapshots/`
([browse](https://central.sonatype.com/service/rest/repository/browse/maven-snapshots/io/opentelemetry/contrib/)).

## Style Guide

See [Style Guide](docs/style-guide.md).

## Pull Request Guidelines

When submitting a pull request, please ensure that you:

- Clearly describe the change and its motivation
- Mention any breaking changes
- Include tests for new functionality
- Follow the [Style Guide](docs/style-guide.md)

## Getting Help

If you need assistance or have questions:

- Post on the [#otel-java](https://cloud-native.slack.com/archives/C014L2KCTE3) Slack channel
- [Open an issue](https://github.com/open-telemetry/opentelemetry-java-contrib/issues/new/choose) in
  this repository
