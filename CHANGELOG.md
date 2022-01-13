# Changelog

## Unreleased

### Updated Libraries

* `opentelemetry-jmx-metrics`
  * Add support for Tomcat
    ([#155](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/155))
  * Add multi attribute support
    ([#137](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/137))

* `opentelemetry-maven-extension`
  * Fix default `service.name` + simplify configuration using Otel AutoConfig SDK 1.10 ResourceProvider SPI improvements (enable specifying the classloader making it compatible with Maven Plexus)
    ([#187](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/187))
  * Fix `service.name` attribute overwrite
    ([#184](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/184))
  * Fix lifecycle to support the Maven daemon
    ([#169](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/169))
  * Capture details on mojo goal executions: `deploy:deploy`, `spring-boot:build-image`, `jib:build`, `snyk:test`, `snyk:monitor`
    ([#146](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/146))
  * Support Maven parallel builds
    ([#161](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/161))

## Version 1.9.0 - 2021-12-03

All libraries updated to OpenTelemetry SDK 1.6.0.

### Updated Libraries

* `opentelemetry-maven-extension`
  * Use Auto Configure Otel SDK Builder
    ([#132](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/132))

* `opentelemetry-aws-xray`
  * Use OkHttp for xray sampling requests
    ([#135](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/135))
  * Use service.name resource attribute instead of span name for service
    ([#138](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/138))
  * X-Ray Sampler: Match rule's HTTP path against http.url attribute if t
    ([#141](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/141))

* `opentelemetry-jfr-streaming`
  * Fix units for some metrics
    ([#140](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/140))
  * Tidy up jfr-streaming
    ([#127](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/127))

## Version 1.8.0 - Skipped

Skipped to stay in sync with other OpenTelemetry Java repositories.

## Version 1.7.0 - 2021-10-29

All libraries updated to OpenTelemetry SDK 1.7.0.

### New Libraries

* `opentelemetry-disruptor-processor` - moved from SDK repo
* `opentelemetry-jfr-streaming` - listens for JFR events (using the Streaming API) and converts them to OpenTelemetry metrics
* `opentelemetry-runtime-attach` - allows programmatic attach of Javaagent

### Updated Libraries

* `opentelemetry-jmx-metrics`
  * Update Cassandra units for latency counters
    ([#111](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/111))
  * Update cassandra counters to be non-monotonic where appropriate
    ([#113](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/113))
  * Update cassandra jmx metrics script to combine similar metrics into labelled metric
    ([#118](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/118))

* `opentelemetry-maven-extension`
  * Support disabling the creation of mojo execution spans
    ([#108](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/108))
  * Use the [OpenTelemetry SDK Autoconfigure extension](https://github.com/open-telemetry/opentelemetry-java/tree/main/sdk-extensions/autoconfigure) to support more configuration setting. All the settings of the OTLP exporter are supported.
    ([#112](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/112))
  * Fix failure to load the extension declaring it in pom.xml
    ([#86](https://github.com/open-telemetry/opentelemetry-java-contrib/issues/86))
  * Fix exception if OTLP exporter is not configured properly
    ([#93](https://github.com/open-telemetry/opentelemetry-java-contrib/issues/93)).

## Version 1.6.0 - 2021-09-22

All libraries updated to OpenTelemetry SDK 1.6.0.

### New Libraries

* `opentelemetry-maven-extension` - this extension can be registered in a Maven build to trace different build steps, for example project build and Maven plugin executions.

## Version 1.5.0 - 2021-09-21

All libraries updated to OpenTelemetry SDK 1.5.0.

### Updated Libraries

* `opentelemetry-jmx-metrics`
  * Due to updating to OpenTelemetry SDK 1.5.0, many of the APIs presented for configuration have been changed so you will need to update any Groovy config scripts to match.
    * `*ValueRecorder` has been replaced with `*Histogram`
    * `*Sum` have been replaced with `*Counter`
    * `*Observer` have been replaced with `*Callback` and do not return any object anymore

## Version 1.4.0 - 2021-08-13

All libraries updated to OpenTelemetry SDK 1.4.0.

### New Libraries

* `opentelemetry-aws-xray` - This library contains OTel extensions for use with [AWS X-Ray](https://docs.aws.amazon.com/xray/index.html).

### Updated Libraries

* `opentelemetry-jmx-metrics`
  * Because of numerous backwards incompatible changes in the OpenTelemetry Metrics data model, you will want to make sure you are running the latest version of the OpenTelemetry collector. Older versions will likely not process metrics correctly.

## Version 1.0.0-alpha - 2021-06-02

### Updated Libraries

* `opentelemetry-jmx-metrics`
  * Adopt OpenTelemetry 1.0.0(-alpha) dependencies (#32)
    * Update JMX Metric Gatherer to use 1.0.0(-alpha) proto, API, SDK, and exporters.
    * Update JMX Metric Gatherer to use Autoconfigure SDK extension properties*
  * JMX Metric Gatherer - Handle missing MBean attributes without failing (#39) - Thanks to @dehaansa.
