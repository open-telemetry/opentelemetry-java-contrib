# Changelog

## Unreleased

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
