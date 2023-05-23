# OpenTelemetry Java Contrib
[![Build](https://github.com/open-telemetry/opentelemetry-java-contrib/actions/workflows/build.yml/badge.svg)](https://github.com/open-telemetry/opentelemetry-java-contrib/actions/workflows/build.yml)

This project is intended to provide helpful libraries and standalone OpenTelemetry-based utilities that don't fit
the express scope of the [OpenTelemetry Java](https://github.com/open-telemetry/opentelemetry-java) or
[Java Instrumentation](https://github.com/open-telemetry/opentelemetry-java-instrumentation) projects.  If you need an
easier way to bring observability to remote JVM-based applications and workflows that isn't easily satisfied by an SDK
feature or via instrumentation, this project is hopefully for you.

## Provided Libraries

* [AWS Resources](./aws-resources/README.md)
* [AWS X-Ray SDK Support](./aws-xray/README.md)
* [AWS X-Ray Propagator](./aws-xray-propagator/README.md)
* [Consistent sampling](./consistent-sampling/README.md)
* [JMX Metric Gatherer](./jmx-metrics/README.md)
* [OpenTelemetry Maven Extension](./maven-extension/README.md)
* [Runtime Attach](./runtime-attach/README.md)
* [Samplers](./samplers/README.md)

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
- [Lauri Tulmin](https://github.com/laurit), Splunk

Maintainers ([@open-telemetry/java-contrib-maintainers](https://github.com/orgs/open-telemetry/teams/java-contrib-maintainers)):

- [Jack Berg](https://github.com/jack-berg), New Relic
- [Mateusz Rzeszutek](https://github.com/mateuszrzeszutek), Splunk
- [Nikita Salnikov-Tarnovski](https://github.com/iNikem), Splunk
- [Trask Stalnaker](https://github.com/trask), Microsoft

Emeritus maintainers:

- [Ryan Fitzpatrick](https://github.com/rmfitzpatrick), Splunk

Learn more about roles in the [community repository](https://github.com/open-telemetry/community/blob/master/community-membership.md).

Thanks to all the people who already contributed!

<a href="https://github.com/open-telemetry/opentelemetry-java-contrib/graphs/contributors">
  <img src="https://contributors-img.web.app/image?repo=open-telemetry/opentelemetry-java-contrib" />
</a>
