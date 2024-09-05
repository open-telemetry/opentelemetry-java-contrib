# OpenTelemetry Java Contrib
[![Build](https://github.com/open-telemetry/opentelemetry-java-contrib/actions/workflows/build.yml/badge.svg)](https://github.com/open-telemetry/opentelemetry-java-contrib/actions/workflows/build.yml)

This project is intended to provide helpful libraries and standalone OpenTelemetry-based utilities that don't fit
the express scope of the [OpenTelemetry Java](https://github.com/open-telemetry/opentelemetry-java) or
[Java Instrumentation](https://github.com/open-telemetry/opentelemetry-java-instrumentation) projects.  If you need an
easier way to bring observability to remote JVM-based applications and workflows that isn't easily satisfied by an SDK
feature or via instrumentation, this project is hopefully for you.

## Provided Libraries

| Status | Library                                                           |
| ------ | ----------------------------------------------------------------- |
| beta   | [AWS Resources](./aws-resources/README.md)                        |
| stable | [AWS X-Ray SDK Support](./aws-xray/README.md)                     |
| beta   | [AWS X-Ray Propagator](./aws-xray-propagator/README.md)           |
| beta   | [Baggage Span Processor](./baggage-processor/README.md)           |
| beta   | [zstd Compressor](./compressors/compressor-zstd/README.md)        |
| beta   | [Consistent Sampling](./consistent-sampling/README.md)            |
| beta   | [Disk Buffering](./disk-buffering/README.md)                      |
| beta   | [GCP Resources](./gcp-resources/README.md)                        |
| beta   | [Inferred Spans](./inferred-spans/README.md)                      |
| beta   | [JFR Connection](./jfr-connection/README.md)                      |
| beta   | [JFR Events](./jfr-events/README.md)                              |
| beta   | [JMX Metric Gatherer](./jmx-metrics/README.md)                    |
| beta   | [Kafka Support](./kafka-exporter/README.md)                       |
| beta   | [OpenTelemetry Maven Extension](./maven-extension/README.md)      |
| beta   | [Micrometer MeterProvider](./micrometer-meter-provider/README.md) |
| beta   | [No-Op API](./noop-api/README.md)                                 |
| beta   | [Intercept and Process Signals Globally](./processors/README.md)  |
| beta   | [Prometheus Client Bridge](./prometheus-client-bridge/README.md)  |
| beta   | [Resource Providers](./resource-providers/README.md)              |
| beta   | [Runtime Attach](./runtime-attach/README.md)                      |
| beta   | [Samplers](./samplers/README.md)                                  |
| beta   | [Span Stacktrace Capture](./span-stacktrace/README.md)            |
| beta   | [Static Instrumenter](./static-instrumenter/README.md)            |

## Getting Started

```bash
# Apply formatting
$ ./gradlew spotlessApply

# Build the complete project
$ ./gradlew build

# Run integration tests
$ ./gradlew integrationTest

# Clean artifacts
$ ./gradlew clean
```

## Contributing

The Java Contrib project was initially formed to provide methods of easy remote JMX metric gathering and reporting,
which is actively in development.  If you have an idea for a similar use case in the metrics, traces, or logging
domain we would be very interested in supporting it.  Please
[open an issue](https://github.com/open-telemetry/opentelemetry-java-contrib/issues/new/choose) to share your idea or
suggestion.  PRs are always welcome and greatly appreciated, but for larger functional changes a pre-coding introduction
can be helpful to ensure this is the correct place and that active or conflicting efforts don't exist.

Triagers ([@open-telemetry/java-contrib-triagers](https://github.com/orgs/open-telemetry/teams/java-contrib-triagers)):

- All [component owners](https://github.com/open-telemetry/opentelemetry-java-contrib/blob/main/.github/component_owners.yml) are given Triager permissions to this repository.

Approvers ([@open-telemetry/java-contrib-approvers](https://github.com/orgs/open-telemetry/teams/java-contrib-approvers)):

- [John Watson](https://github.com/jkwatson), Verta.ai

Maintainers ([@open-telemetry/java-contrib-maintainers](https://github.com/orgs/open-telemetry/teams/java-contrib-maintainers)):

- [Jack Berg](https://github.com/jack-berg), New Relic
- [Jason Plumb](https://github.com/breedx-splk), Splunk
- [Lauri Tulmin](https://github.com/laurit), Splunk
- [Trask Stalnaker](https://github.com/trask), Microsoft

Emeritus maintainers:

- [Mateusz Rzeszutek](https://github.com/mateuszrzeszutek)
- [Nikita Salnikov-Tarnovski](https://github.com/iNikem), Splunk
- [Ryan Fitzpatrick](https://github.com/rmfitzpatrick), Splunk

Learn more about roles in the [community repository](https://github.com/open-telemetry/community/blob/main/guides/contributor/membership.md).

Thanks to all the people who already contributed!

<a href="https://github.com/open-telemetry/opentelemetry-java-contrib/graphs/contributors">
  <img src="https://contributors-img.web.app/image?repo=open-telemetry/opentelemetry-java-contrib" />
</a>
