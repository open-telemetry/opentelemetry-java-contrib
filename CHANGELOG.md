# Changelog

## Unreleased

### JFR streaming

- Add buffer handlers and implement buffer metrics
  ([#650](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/650))
- Implement GC duration metric
  ([#653](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/653))
- Implement memory metrics
  ([#652](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/652))

### Prometheus client bridge - New 🌟

This module can be used to bridge OpenTelemetry metrics into the `prometheus-simpleclient` library.

## Version 1.21.0 (2022-12-15)

### JFR streaming

- Update handlers in jfr-streaming to match spec
  ([#616](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/616))

## Version 1.20.1 (2022-11-22)

### 🛠️ Bug fixes

- Previously targeted OpenTelemetry Instrumentation version had never been updated to target
  OpenTelemetry SDK 1.20

## Version 1.20.0 (2022-11-17)

### AWS X-Ray propagator

- Move io.opentelemetry:opentelemetry-extension-aws to contrib
  ([#547](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/547))

### JFR streaming

- Thread count and classes loaded handlers
  ([#571](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/571))

### Resource providers

- Webapp service name detector
  ([#562](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/562))
- Glassfish service name detector
  ([#579](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/579))
- Add remaining app server service name detectors
  ([#583](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/583))

## Version 1.19.1 (2022-10-16)

### AWS resources

- Fixed artifact `io.opentelemetry.contrib:opentelemetry-aws-resources` not being published to
  maven central
  ([#535](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/535))

## Version 1.19.0 (2022-10-14)

### Runtime attach

- Fix missing class in opentelemetry-runtime-attach jar
  ([#509](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/509))

## Version 1.18.0 (2022-09-19)

### AWS resources

Relocated from the opentelemetry-java repository, and now published under the coordinates
`io.opentelemetry.contrib:opentelemetry-aws-resources`

### JFR events

Relocated from the opentelemetry-java repository, and now published under the coordinates
`io.opentelemetry.contrib:opentelemetry-jfr-events`

### No-op API

Relocated from the opentelemetry-java repository, and now published under the coordinates
`io.opentelemetry.contrib:opentelemetry-noop-api`

## Version 1.17.0 (2022-08-19)

### Consistent sampling

- Support traceId-based r-values
  ([#417](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/417))

### Runtime attach

- Prevent the runtime attachment from launching multiple times
  ([#409](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/409))

## Version 1.16.0 (2022-07-20)

### AWS X-Ray

- Fix #376: `AwsXrayRemoteSampler` doesn’t poll for update
  ([#377](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/377))

### Runtime attach

- Fix "Class path contains multiple SLF4J bindings"
  ([#380](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/380))
- Improve exception handling and documentation
  ([#388](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/388))

## Version 1.15.0 (2022-06-17)

### Consistent sampling

- ConsistentSampler does not unset p from tracestate when deciding not to sample
  ([#350](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/350))
- Add consistent reservoir sampling span processor
  ([#352](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/352))

### Micrometer MeterProvider - New 🌟

This utility provides an implementation of `MeterProvider` which wraps a Micrometer `MeterRegistry`
and delegates the reporting of all metrics through Micrometer. This enables projects which already
rely on Micrometer and cannot currently migrate to OpenTelemetry Metrics to be able to report on
metrics that are reported through the OpenTelemetry Metrics API.

### Runtime attach

- Do not attach if not requested from the main method on the main thread
  ([#354](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/354))
- Fix "URI is not hierarchical" during runtime attachment
  ([#359](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/359))

## Version 1.14.0 (2022-05-19)

All components updated to target OpenTelemetry SDK 1.14.0.

### JMX metrics

- Support setting more properties in the JMX Metrics properties file
  ([#323](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/323))
- Add Jetty Integration
  ([#320](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/320))

## Version 1.13.0 (2022-04-20)

All components updated to target OpenTelemetry SDK 1.13.0.

### Consistent sampling - New 🌟

This component adds various Sampler implementations for consistent sampling as defined by
https://github.com/open-telemetry/opentelemetry-specification/blob/main/specification/trace/tracestate-probability-sampling.md
and https://github.com/open-telemetry/opentelemetry-specification/pull/2047.

## Version 1.12.0 (2022-03-14)

All components updated to target OpenTelemetry SDK 1.12.0.

## Version 1.11.0 (2022-03-03)

All components updated to target OpenTelemetry SDK 1.11.0.

### JFR streaming

- Split up GC Handlers, add support for Parallel
  ([#201](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/201))

### JMX metrics

- jmx-metrics: Activemq
  ([#188](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/188))
- Adds Solr metrics gathering to jmx-metrics
  ([#204](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/204))
- Add Hadoop Monitoring
  ([#210](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/210))
- Add Hbase Support
  ([#211](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/211))
- Update Kafka JMX Script
  ([#216](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/216))
- Fixes solr JMX metrics to use all possible MBeans instead of only first
  ([#223](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/223))
- WildFly Monitoring
  ([#224](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/224))
- Updates Solr cache size metric to point to new attribute to measure byte size
  ([#225](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/225))
- Updates jmx-metrics WildFly integration to point to integer attributes for some metrics
  ([#232](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/232))
- Update file total metric
  ([#234](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/234))

## Version 1.10.0 (2022-01-18)

All components updated to target OpenTelemetry SDK 1.10.0.

### JMX metrics

- Add multi attribute support
  ([#137](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/137))
- Add support for Tomcat
  ([#155](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/155))
- Change metric to Gauge
  ([#194](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/194))
- Remove manual exporter flush
  ([#190](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/190))

### Maven extension

- Support Maven parallel builds
  ([#161](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/161))
- Capture details on mojo goal executions: `deploy:deploy`, `spring-boot:build-image`, `jib:build`
  , `snyk:test`, `snyk:monitor`
  ([#146](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/146))
- Fix lifecycle to support the Maven daemon
  ([#169](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/169))
- Fix `service.name` attribute overwrite
  ([#184](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/184))
- Fix default `service.name` + simplify configuration using Otel AutoConfig SDK 1.10
  ResourceProvider SPI improvements (enable specifying the classloader making it compatible with
  Maven Plexus)
  ([#187](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/187))
- Add Tracer instrumentationVersion (ie `otel.library.version`)
  ([#191](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/191))
- Reduce the cardinality of mojo span names
  ([#192](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/192))

### Samplers

- Rename contrib-samplers to samplers
  ([#185](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/185))

## Version 1.9.0 (2021-12-03)

All components updated to target OpenTelemetry SDK 1.9.1.

### Maven extension

- Use Auto Configure Otel SDK Builder
  ([#132](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/132))

### AWS X-Ray

- Use OkHttp for xray sampling requests
  ([#135](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/135))
- Use service.name resource attribute instead of span name for service
  ([#138](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/138))
- X-Ray Sampler: Match rule's HTTP path against http.url attribute if t
  ([#141](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/141))

### JFR streaming

- Tidy up jfr-streaming
  ([#127](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/127))
- Fix units for some metrics
  ([#140](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/140))

## Version 1.8.0 - Skipped

Skipped to stay in sync with other OpenTelemetry Java repositories.

## Version 1.7.0 (2021-10-29)

All components updated to target OpenTelemetry SDK 1.7.0.

### Disruptor span processor - New 🌟

Moved from SDK repo.

### JFR streaming - New 🌟

Listens for JFR events (using the Streaming API) and converts them to OpenTelemetry metrics.

### Runtime attach - New 🌟

Allows programmatic attach of Javaagent.

### JMX metrics

- Update Cassandra units for latency counters
  ([#111](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/111))
- Update cassandra counters to be non-monotonic where appropriate
  ([#113](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/113))
- Update cassandra jmx metrics script to combine similar metrics into labelled metric
  ([#118](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/118))

### Maven extension

- Fix failure to load the extension declaring it in pom.xml
  ([#86](https://github.com/open-telemetry/opentelemetry-java-contrib/issues/86))
- Fix exception if OTLP exporter is not configured properly
  ([#93](https://github.com/open-telemetry/opentelemetry-java-contrib/issues/93)).
- Support disabling the creation of mojo execution spans
  ([#108](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/108))
- Use
  the [OpenTelemetry SDK Autoconfigure extension](https://github.com/open-telemetry/opentelemetry-java/tree/main/sdk-extensions/autoconfigure)
  to support more configuration setting. All the settings of the OTLP exporter are supported.
  ([#112](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/112))

## Version 1.6.0 (2021-09-22)

All components updated to target OpenTelemetry SDK 1.6.0.

### Maven extension - New 🌟

This extension can be registered in a Maven build to trace different build steps, for example
project build and Maven plugin executions.

## Version 1.5.0 (2021-09-21)

All components updated to target OpenTelemetry SDK 1.5.0.

### JMX metrics

- Due to updating to OpenTelemetry SDK 1.5.0, many of the APIs presented for configuration have been
  changed so you will need to update any Groovy config scripts to match.
  * `*ValueRecorder` has been replaced with `*Histogram`
  * `*Sum` have been replaced with `*Counter`
  * `*Observer` have been replaced with `*Callback` and do not return any object anymore

## Version 1.4.0 (2021-08-13)

All components updated to target OpenTelemetry SDK 1.4.0.

### AWS X-Ray - New 🌟

This library contains OTel extensions for use
with [AWS X-Ray](https://docs.aws.amazon.com/xray/index.html).

### JMX Metrics

- Because of numerous backwards incompatible changes in the OpenTelemetry Metrics data model, you
  will want to make sure you are running the latest version of the OpenTelemetry collector. Older
  versions will likely not process metrics correctly.

## Version 1.0.0-alpha (2021-06-02)

### JMX metrics - New 🌟

- Adopt OpenTelemetry 1.0.0(-alpha) dependencies (#32)
  * Update JMX Metric Gatherer to use 1.0.0(-alpha) proto, API, SDK, and exporters.
  * Update JMX Metric Gatherer to use Autoconfigure SDK extension properties*
- JMX Metric Gatherer - Handle missing MBean attributes without failing (#39) - Thanks to @dehaansa.
