# OpenTelemetry Java Contrib
[![Build status](https://img.shields.io/circleci/build/github/open-telemetry/opentelemetry-java-contrib?style=for-the-badge)](https://app.circleci.com/pipelines/github/open-telemetry/opentelemetry-java-contrib?branch=master)

This project is intended to provide helpful libraries and standalone OpenTelemetry-based utilities that don't fit
the express scope of the [OpenTelemetry Java](https://github.com/open-telemetry/opentelemetry-java) or
[Java Instrumentation](https://github.com/open-telemetry/opentelemetry-java-instrumentation) projects.  If you need an
easier way to bring observability to remote JVM-based applications and workflows that isn't easily satisfied by an SDK
feature or via instrumentation, this project is hopefully for you.

*This project is in its early stages and doesn't provide any assurances of stability or production readiness.*


## Provided Libraries

* [JMX Metric Gatherer](./contrib/jmx-metrics/README.md)

## Getting Started

```bash
# Build the complete project
$ make build

# Run integration tests
$ make integration-test

# Clean artifacts
$ make clean

# Apply formatting
$ make lint
```

## Contributing

The Java Contrib project was initially formed to provide methods of easy remote JMX metric gathering and reporting,
which is actively in development.  If you have an idea for a similar use case in the metrics, traces, or logging
domain we would be very interested in supporting it.  Please
[open an issue](https://github.com/open-telemetry/opentelemetry-java-contrib/issues/new/choose) to share your idea or
suggestion.  PRs are always welcome and greatly appreciated, but for larger functional changes a pre-coding introduction
can be helpful to ensure this is the correct place and that active or conflicting efforts don't exist.

## Owners

- [Anuraag Agrawal](https://github.com/anuraaga), AWS
- [Pablo Collins](https://github.com/pmcollins), Splunk
- [Ryan Fitzpatrick](https://github.com/rmfitzpatrick), Splunk (maintainer)
- [Trask Stalnaker](https://github.com/trask), Microsoft

For more information on the OpenTelemetry community please see the
[community content project](https://github.com/open-telemetry/community).
