[![Release](https://img.shields.io/github/v/release/open-telemetry/opentelemetry-java-contrib?include_prereleases&style=)](https://github.com/open-telemetry/opentelemetry-java-contrib/releases/)
[![OpenSSF Scorecard](https://api.scorecard.dev/projects/github.com/open-telemetry/opentelemetry-java-contrib/badge)](https://scorecard.dev/viewer/?uri=github.com/open-telemetry/opentelemetry-java-contrib)
[![OpenSSF Best Practices](https://www.bestpractices.dev/projects/9992/badge)](https://www.bestpractices.dev/projects/9992)
[![Slack](https://img.shields.io/badge/slack-@cncf/otel--java-blue.svg?logo=slack)](https://cloud-native.slack.com/archives/C014L2KCTE3)

# OpenTelemetry Java Contrib

This project is intended to provide helpful libraries and standalone OpenTelemetry-based utilities that don't fit
the express scope of the [OpenTelemetry Java](https://github.com/open-telemetry/opentelemetry-java) or
[Java Instrumentation](https://github.com/open-telemetry/opentelemetry-java-instrumentation) projects.  If you need an
easier way to bring observability to remote JVM-based applications and workflows that isn't easily satisfied by an SDK
feature or via instrumentation, this project is hopefully for you.

## Provided Libraries

| Status* | Library                                                           |
| ------- |-------------------------------------------------------------------|
| beta    | [AWS Resources](./aws-resources/README.md)                        |
| stable  | [AWS X-Ray SDK Support](./aws-xray/README.md)                     |
| alpha   | [AWS X-Ray Propagator](./aws-xray-propagator/README.md)           |
| alpha   | [Baggage Processors](./baggage-processor/README.md) |
| alpha   | [zstd Compressor](./compressors/compressor-zstd/README.md)        |
| alpha   | [Consistent Sampling](./consistent-sampling/README.md)            |
| alpha   | [Disk Buffering](./disk-buffering/README.md)                      |
| alpha   | [GCP Authentication Extension](./gcp-auth-extension/README.md)    |
| beta    | [GCP Resources](./gcp-resources/README.md)                        |
| beta    | [Inferred Spans](./inferred-spans/README.md)                      |
| alpha   | [JFR Connection](./jfr-connection/README.md)                      |
| alpha   | [JFR Events](./jfr-events/README.md)                              |
| alpha   | [JMX Metric Gatherer](./jmx-metrics/README.md)                    |
| alpha   | [Kafka Support](./kafka-exporter/README.md)                       |
| alpha   | [OpenTelemetry Maven Extension](./maven-extension/README.md)      |
| alpha   | [Micrometer MeterProvider](./micrometer-meter-provider/README.md) |
| alpha   | [No-Op API](./noop-api/README.md)                                 |
| alpha   | [Intercept and Process Signals Globally](./processors/README.md)  |
| alpha   | [Prometheus Client Bridge](./prometheus-client-bridge/README.md)  |
| alpha   | [Resource Providers](./resource-providers/README.md)              |
| alpha   | [Runtime Attach](./runtime-attach/README.md)                      |
| alpha   | [Samplers](./samplers/README.md)                                  |
| beta    | [Span Stacktrace Capture](./span-stacktrace/README.md)            |
| alpha   | [Static Instrumenter](./static-instrumenter/README.md)            |

\* `alpha`, `beta` and `stable` are currently used to denote library status per [otep 0232](https://github.com/open-telemetry/oteps/blob/main/text/0232-maturity-of-otel.md).
To reach stable status, the library needs to have stable APIs, stable semantic conventions, and be production ready.
On reaching stable status, the `otel.stable` value in `gradle.properties` should be set to `true`.
Note that currently all the libraries are released together with the version of this repo, so breaking changes (after stable
status is reached) would bump the major version of all libraries together. This could get complicated so `stable` has a high bar.

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
- [Nikita Salnikov-Tarnovski](https://github.com/iNikem)
- [Ryan Fitzpatrick](https://github.com/rmfitzpatrick)

Learn more about roles in the [community repository](https://github.com/open-telemetry/community/blob/main/guides/contributor/membership.md).

Thanks to all the people who already contributed!

<a href="https://github.com/open-telemetry/opentelemetry-java-contrib/graphs/contributors">
  <img src="https://contributors-img.web.app/image?repo=open-telemetry/opentelemetry-java-contrib" />
</a>
