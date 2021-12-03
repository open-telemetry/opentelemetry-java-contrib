# Changelog

## Unreleased

## Version 1.9.0

* [Maven Extension] Update README, bump version to 1.7.0-alpha by @cyrille-leclerc in https://github.com/open-telemetry/opentelemetry-java-contrib/pull/129
* Use OkHttp for xray sampling requests. by @anuraaga in https://github.com/open-telemetry/opentelemetry-java-contrib/pull/135
* Use service.name resource attribute instead of span name for service … by @anuraaga in https://github.com/open-telemetry/opentelemetry-java-contrib/pull/138
* X-Ray Sampler: Match rule's HTTP path against http.url attribute if t… by @anuraaga in https://github.com/open-telemetry/opentelemetry-java-contrib/pull/141
* [Maven-Extension] Use Auto Configure Otel SDK Builder by @cyrille-leclerc in https://github.com/open-telemetry/opentelemetry-java-contrib/pull/132
* Fix units for some metrics by @kittylyst in https://github.com/open-telemetry/opentelemetry-java-contrib/pull/140
* Tidy up jfr-streaming by @jack-berg in https://github.com/open-telemetry/opentelemetry-java-contrib/pull/127

## Version 1.7.0

* All libraries updated to OpenTelemetry SDK 1.7.0.

### New Libraries

* [`opentelemetry-disruptor-processor`](https://github.com/open-telemetry/opentelemetry-java-contrib/tree/main/jfr-streaming) - moved from SDK repo

* [`opentelemetry-jfr-streaming`](https://github.com/open-telemetry/opentelemetry-java-contrib/tree/main/jfr-streaming) - listens for JFR events (using the Streaming API) and converts them to OpenTelemetry metrics

* [`opentelemetry-runtime-attach`](https://github.com/open-telemetry/opentelemetry-java-contrib/tree/main/runtime-attach) - allows programmatic attach of Javaagent

### Updated Libraries

* [`opentelemetry-jmx-metrics`](https://github.com/open-telemetry/opentelemetry-java-contrib/tree/main/jmx-metrics)
  * Update Cassandra units for latency counters
    [#111](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/111)
  * Update cassandra counters to be non-monotonic where appropriate
    [#113](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/113)
  * Update cassandra jmx metrics script to combine similar metrics into labelled metric
    [#118](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/118)

* [`opentelemetry-maven-extension`](https://github.com/open-telemetry/opentelemetry-java-contrib/tree/main/maven-extension)
  * Support disabling the creation of mojo execution spans
    [#108](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/108)
  * Use the [OpenTelemetry SDK Autoconfigure extension](https://github.com/open-telemetry/opentelemetry-java/tree/main/sdk-extensions/autoconfigure) to support more configuration setting
    [#112](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/112). All the settings of the OTLP exporter are supported.
  * Fix failure to load the extension declaring it in pom.xml
    [#86](https://github.com/open-telemetry/opentelemetry-java-contrib/issues/86)
  * Fix exception if OTLP exporter is not configured properly
    [#93](https://github.com/open-telemetry/opentelemetry-java-contrib/issues/93).

## Version 1.6.0 - 2021-09-22

* All libraries updated to OpenTelemetry SDK 1.6.0.

### New Libraries

* [`opentelemetry-maven-extension`](https://github.com/open-telemetry/opentelemetry-java-contrib/tree/main/maven-extension) - this extension can be registered in a Maven build to trace different build steps, for example project build and Maven plugin executions.

## Version 1.5.0 - 2021-09-21

* All libraries updated to OpenTelemetry SDK 1.5.0.

### Updated Libraries

* [`opentelemetry-jmx-metrics`](https://github.com/open-telemetry/opentelemetry-java-contrib/tree/main/jmx-metrics)
  * Due to updating to OpenTelemetry SDK 1.5.0, many of the APIs presented for configuration have been changed so you will need to update any Groovy config scripts to match.
    * `*ValueRecorder` has been replaced with `*Histogram`
    * `*Sum` have been replaced with `*Counter`
    * `*Observer` have been replaced with `*Callback` and do not return any object anymore

## Version 1.4.0 - 2021-08-13

* All libraries updated to OpenTelemetry SDK 1.4.0.

### New Libraries

* [`opentelemetry-aws-xray`](https://github.com/open-telemetry/opentelemetry-java-contrib/tree/main/aws-xray) - This library contains OTel extensions for use with [AWS X-Ray](https://docs.aws.amazon.com/xray/index.html).

### Updated Libraries

* [`opentelemetry-jmx-metrics`](https://github.com/open-telemetry/opentelemetry-java-contrib/tree/main/jmx-metrics)
  * Because of numerous backwards incompatible changes in the OpenTelemetry Metrics data model, you will want to make sure you are running the latest version of the OpenTelemetry collector. Older versions will likely not process metrics correctly.

## Version 1.0.0-alpha - 2021-06-02

### Updated Libraries

* [`opentelemetry-jmx-metrics`](https://github.com/open-telemetry/opentelemetry-java-contrib/tree/main/jmx-metrics)
  * Adopt OpenTelemetry 1.0.0(-alpha) dependencies (#32)
    * Update JMX Metric Gatherer to use 1.0.0(-alpha) proto, API, SDK, and exporters.
    * Update JMX Metric Gatherer to use Autoconfigure SDK extension properties*
  * JMX Metric Gatherer - Handle missing MBean attributes without failing (#39) - Thanks to @dehaansa.
