# Changelog

## Unreleased

### AWS X-Ray propagator

- Update xray lambda component provider name
  ([#2423](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/2423))

### Inferred spans

- Add declarative config support.
  ([#2030](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/2030))
- Fix occasional/sporadic NPE.
  ([#2443](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/2443))

### Span stack traces

- Fix stacktrace processor name for declarative config.
  ([#2415](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/2415))

## Version 1.51.0 (2025-10-20)

### AWS X-Ray SDK support and propagator

- Add AWS X-Ray adaptive sampling support
  ([#2147](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/2147)).

### Common Expression Language sampler - New 🌟

A rule-based sampler backed by Common Expression Language (CEL)
expressions for declarative sampling rules

### Disk buffering

- Implement the disk buffering API
  ([#2183](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/2183)).

### Inferred spans

- Return the previous profiler interval from `setInterval`
  ([#2354](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/2354)).

### OpAMP client

- Restore the client parameter to OpAMP callbacks
  ([#2336](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/2336)).

## Version 1.50.0 (2025-09-26)

Note: This release broadly applies some style guidelines across the repository. As a result,
some classes that were visible might be package/private. Other non-final classes may now
be final. See
[#2182](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/2182)
and
[#2210](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/2210)
and
[#2212](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/2212)
and
[#2213](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/2212)
for examples and details. These changes are not expected to break anyone, so please open
an issue if this causes problems.

### Baggage processor

- Move baggage processor to the front of the processor list
  ([#2152](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/2152))
- Add declarative configuration support
  ([#2031](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/2031))

### Disk buffering

- Catching IllegalStateException in case of failed deserialization
  ([#2157](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/2157))
- Apply final to public API classes where possible
  ([#2216](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/2216))
- Handle empty attribute values
  ([#2268](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/2268))

### Inferred spans

- Support dynamically changing the inferred span interval
  ([#2153](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/2153))

### JMX scraper

- Implement stable `service.instance.id`
  ([#2270](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/2270))

### Kafka exporter

- Add Kafka connectivity error handling
  ([#2202](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/2202))

### OpAMP client

- Move important user-facing classes out of 'internal' package
  ([#2249](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/2249))
- Exponential backoff retries on http connection failures
  ([#2274](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/2274))

### Span stack traces

- Add declarative configuration support
  ([#2262](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/2262))


## Version 1.49.0 (2025-08-25)

### Consistent sampling

- Add updateable threshold sampler for dynamic sampling configuration
  ([#2137](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/2137))

### Disk buffering

- Introduce API changes for improved disk buffering functionality
  ([#2084](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/2084))
- Implement more efficient serializer with direct disk write capabilities
  ([#2138](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/2138))

### IBM MQ metrics - New 🌟

IBM MQ metrics collection utility.

### Inferred spans

- Update async profiler to version 4.1 for improved performance
  ([#2096](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/2096))

### OpAMP client - New 🌟

OpenTelemetry Agent Management Protocol (OpAMP) client implementation.

## Version 1.48.0 (2025-07-23)

### AWS resources

- Support for declarative configuration
  ([#2014](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/2014))

### AWS X-Ray SDK support

- Update SamplerRulesApplier to recognize new HTTP/URL semconv
  ([#1959](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/1959))

### Azure resources

- Support for declarative configuration
  ([#2014](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/2014))

### CloudFoundry resources

- Support for declarative configuration
  ([#2014](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/2014))

### Consistent sampling

- Refactor ConsistentFixedThresholdSampler to prepare for dynamic threshold support
  ([#2018](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/2018))
- ConsistentRateLimitingSampler can fail if used in combination with legacy samplers
  ([#2022](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/2022))

### GCP resources

- Support for declarative configuration
  ([#2014](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/2014))

### JMX metrics

- Deprecate JMX Gatherer and provide migration guide to JMX Scraper
  ([#2034](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/2034))

### JMX scraper

- Update Jetty metrics configuration corresponding to Java Instrumentation 2.18.0
  ([#2033](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/2033))
- Mark as production-ready and remove experimental status
  ([#2034](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/2034))

### Maven extension

- Support for declarative configuration
  ([#2014](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/2014))

### Resource providers

- Support for declarative configuration
  ([#2014](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/2014))

## Version 1.47.0 (2025-07-04)

### Disk buffering

- Shared storage
  ([#1912](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/1912))
- Implementing ExtendedLogRecordData
  ([#1918](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/1918))
- Add missing EventName to disk-buffering LogRecordDataMapper
  ([#1950](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/1950))

### GCP authentication extension

- Update the internal implementation such that the required headers are retrieved
  from the Google Auth Library instead of manually constructing and passing them.
  ([#1860](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/1860))
- Add metrics support to auth extension
  ([#1891](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/1891))
- Update ConfigurableOptions to read from ConfigProperties
  ([#1904](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/1904))

### Inferred spans

- Upgrade async-profiler to 4.0
  ([#1872](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/1872))

### Kafka exporter

- Upgrade kafka-clients to 4.0 (and so now requires Java 11+)
  ([#1802](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/1802))

### Maven extension

- Add option to record transferred artifacts
  ([#1875](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/1875))

## Version 1.46.0 (2025-04-11)

### Baggage processor

- Remove the deprecated and unused bare Predicate
  ([#1828](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/1828))

### Telemetry processors

- Add logs filtering
  ([#1823](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/1823))

## Version 1.45.0 (2025-03-14)

### Disk buffering

- Make configuration package public
  ([#1781](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/1781))

### JMX scraper

- Reuse instrumentation metrics by default
  ([#1782](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/1782))

## Version 1.44.0 (2025-02-21)

### AWS resources

- Changed resource attribute `container.image.tag` to `container.image.tags`
  ([#1736](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/1736))

### AWS X-Ray propagator

- Make `xray-lambda` propagator available via SPI
  ([#1669](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/1669))
- Support Lineage in XRay trace header and remove additional baggage from being added
  ([#1671](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/1671))

### CloudFoundry resources - New 🌟

CloudFoundry resource detector.

### Disk buffering

- Use delegate's temporality
  ([#1672](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/1672))

### GCP authentication extension

- Publish both shaded and unshaded variants
  ([#1688](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/1688))

### JMX metrics

- Updated Hadoop metric unit definitions to align with semantic conventions
  ([#1675](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/1675))
- Updated Kafka metric unit definitions to align with semantic conventions
  ([#1670](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/1670))

### JMX scraper

- Use SDK autoconfigure module
  ([#1651](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/1651))
- Rename `otel.jmx.custom.scraping.config` to `otel.jmx.config` in order to align
  with `io.opentelemetry.instrumentation:opentelemetry-jmx-metrics`
  ([#1678](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/1678))
- Hadoop metrics added
  ([#1675](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/1675))
- Add a CLI option to test the connection
  ([#1684](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/1684))
- Kafka server, producer, and consumer metrics added
  ([#1670](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/1670))
- Add custom YAML support
  ([#1741](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/1741))
- Add SSL support
  ([#1710](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/1710))
- Replicate JMXMP/SASL config from the JMX metrics module
  ([#1749](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/1749))

### Maven extension

- Support Maven 4.0
  ([#1679](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/1679))

### Processors

- Changed `EventToSpanEventBridge` from reading `event.name` to reading the new LogRecord
  [EventName](https://github.com/open-telemetry/opentelemetry-specification/blob/main/specification/logs/data-model.md#field-eventname)
  field.
  ([#1736](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/1736))

### Static instrumenter

- Module has been removed
  ([#1755](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/1755))

## Version 1.43.0 (2025-01-17)

### Azure resources - New 🌟

Azure resource detectors.

### Consistent sampling

- Improve interop with legacy samplers
  ([#1629](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/1629))

### GCP authentication extension - New 🌟

Allows users to export telemetry from their applications to Google Cloud using the built-in OTLP exporters.
The extension takes care of the necessary configuration required to authenticate to GCP to successfully export telemetry.

### JMX scraper

- Add support for Solr
  ([#1595](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/1595))

## Version 1.42.0 (2024-12-13)

### AWS X-Ray SDK support

- Update semconv dependency version
  ([#1585](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/1585))

### Baggage processor

- [baggage-processor] Add BaggageLogRecordProcessor
  ([#1576](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/1576))

### Disk buffering

- Deserialization validation
  ([#1571](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/1571))

### JMX metrics

- Align HBase metric units to semconv
  ([#1538](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/1538))
- Align Cassandra metric units to semconv
  ([#1591](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/1591))
- Align Tomcat metric units to semconv
  ([#1589](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/1589))
- Align JVM units to semconv
  ([#1593](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/1593))

### JMX scraper - New 🌟

The future of the [JMX metrics](./jmx-metrics/README.md) component,
built on top of the
[JMX metrics](https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/main/instrumentation/jmx-metrics/README.md#jmx-metric-insight)
component from the opentelemetry-java-instrumentation repository.

### Maven extension

- Load OTel SDK config from environment variables and system properties
  ([#1434](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/1434))
- Workaround `NoClassDefFoundError` in `@PreDestroy` waiting for MNG-7056
  ([#1431](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/1431))

## Version 1.41.0 (2024-11-21)

### JMX metrics

- Align ActiveMQ metric units to semconv
  ([#1553](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/1553))
- Align Jetty metric units to semconv
  ([#1517](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/1517))

### Inferred spans

- Allow customization of parent-override behavior
  ([#1533](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/1533))

### Telemetry processors

- Add LogRecordProcessor to record event log records as span events
  ([#1551](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/1551))

## Version 1.40.0 (2024-10-18)

### AWS X-Ray SDK support

- Ensure all XRay Sampler functionality is under ParentBased logic
  ([#1488](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/1488))

### GCP Resources

- Add gcr job support
  ([#1462](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/1462))

### Inferred spans

- Rename param and description to proper value
  ([#1486](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/1486))

### JFR connection

- Fix wrong parameter sent to JFR DiagnosticCommand
  ([#1492](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/1492))

### Span stack traces

- Support autoconfigure
  ([#1499](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/1499))

## Version 1.39.0 (2024-09-17)

### AWS X-Ray propagator

- Handle too short `X-Amzn-Trace-Id` header
  ([#1036](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/1036))
- Add declarative config support for aws xray propagators
  ([#1442](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/1442))

### AWS X-Ray SDK support

- Fix native mode error cause by static init of random
  ([#862](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/862))

### Consistent sampling

- Composite Samplers prototype
  ([#1443](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/1443))

### Disk buffering

- Add debug mode for verbose logging
  ([#1455](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/1455))

### GCP Resources

- Fix incorrect `cloud.platform` value for GCF
  ([#1454](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/1454))

### JMX metrics

- Add option to aggregate across multiple MBeans
  ([#1366](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/1366))

### Samplers

- Add declarative config support for `RuleBasedRoutingSampler`
  ([#1440](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/1440))

### Span stack traces

- Add config option `otel.java.experimental.span-stacktrace.min.duration`
  ([#1414](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/1414))

## Version 1.38.0 (2024-08-19)

### JFR connection

- Recording close should not throw exception
  ([#1412](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/1412))

## Version 1.37.0 (2024-07-18)

### AWS resources

- Add ECS cluster detection
  ([#1354](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/1354))

### Baggage processor

- Add config support
  ([#1330](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/1330))

### Inferred spans - New 🌟

An OpenTelemetry extension for generating spans via profiling instead of instrumentation.
This extension enhances traces by
running [async-profiler](https://github.com/async-profiler/async-profiler) in wall-clock profiling
mode
whenever there is an active sampled OpenTelemetry span.

The resulting profiling data is analyzed afterward and spans are "inferred".
This means there is a delay between the regular and the inferred spans being visible
in your OpenTelemetry backend/UI.

### JFR connection

- Fix for using diagnostic command to start a recording
  ([#1352](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/1352))

### JMX metrics

- Support both a script and target systems
  ([#1339](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/1339))

## Version 1.36.0 (2024-05-29)

### AWS resources

- Optimization: don't attempt detection if a cloud provider has already been detected
  ([#1225](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/1225))

### Baggage processor - New 🌟

This module provides a SpanProcessor that stamps baggage onto spans as attributes on start.

### Consistent sampling

- Assume random trace ID and set th-field only for sampled spans
  ([#1278](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/1278))

### GCP Resources

- Optimization: don't attempt detection if a cloud provider has already been detected
  ([#1225](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/1225))
- Update guidance for manual instrumentation usage
  ([#1250](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/1250))

### JMX metrics

- Remove `slf4j-simple` dependency
  ([#1283](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/1283))

### Maven extension

- Disable metrics and logs by default
  ([#1276](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/1276))
- Migrate to current semconv
  ([#1299](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/1299))
- Migrate from Plexus to JSR 330 dependency injection APIs
  ([#1320](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/1320))

### Span stack trace

- Enable publishing to maven central
  ([#1297](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/1297))

## Version 1.35.0 (2024-04-16)

### JMX metrics

- Add support for newly named Tomcat MBean with Spring
  ([#1269](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/1269))

### Span stack traces - New 🌟

This module provides a SpanProcessor that captures stack traces on spans that meet
a certain criteria such as exceeding a certain duration threshold.

## Version 1.34.0 (2024-03-27)

### AWS resources

- Add support for `cloud.account.id`, `cloud.availability_zone`, `cloud.region`,
  and `cloud.resource_id`
  ([#1171](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/1171))

### AWS X-Ray propagator

- Add xray propagators that prioritizes xray environment variable
  ([#1032](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/1032))

### GCP Resources

- Update docs on how to use with Java agent v2.2.0 and later
  ([#1237](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/1237))

### Micrometer MeterProvider

- Implement Metrics incubator APIs to accept advice
  ([#1190](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/1190))

## Version 1.33.0 (2024-02-21)

### Compressors

- Add zstd compressor implementation for OTLP exporters
  ([#1108](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/1108))

### Consistent sampling

- Switch from acceptance to rejection threshold
  ([#1130](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/1130))

### Disk buffering

- Shadowing generated proto java sources
  ([#1146](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/1146))
- Single responsibility for disk exporters
  ([#1161](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/1161))
- Split serializer
  ([#1167](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/1167))
- Disk buffering config and README updates
  ([#1169](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/1169))
- Ensure no sign propagation for flags byte
  ([#1166](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/1166))

### GCP Resources - New 🌟

This module provides GCP resource detectors for OpenTelemetry.

### JMX metrics

- Add Error handling for closure parameters
  ([#1102](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/1102))
- Add `kafka.request.time.avg`
  ([#1135](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/1135))

### Kafka exporter - New 🌟

This module contains `KafkaSpanExporter`, which is an implementation of the
`io.opentelemetry.sdk.trace.export.SpanExporter` interface.

`KafkaSpanExporter` can be used for sending `SpanData` to a Kafka topic.

## Version 1.32.0 (2023-11-27)

### Disk buffering

- Using Android 21 as minimum supported for disk-buffering
  ([#1096](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/1096))

## Version 1.31.0 (2023-10-18)

### Consistent sampling

- Explicitly pass invalid p-value to root sampler
  ([#1053](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/1053))
- Consistent sampler prototypes using 56 bits of randomness
  ([#1063](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/1063))

### Runtime attach

- Rename runtime attach method from `attachJavaagentToCurrentJVM`
  to `attachJavaagentToCurrentJvm`
  ([#1077](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/1077))

### Samplers

- Support `thread.name` attributes in RuleBasedRoutingSampler
  ([#1030](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/1030))

## Version 1.30.0 (2023-09-18)

### Disk buffering

- Remove protobuf dependency
  ([#1008](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/1008))

### Maven extension

- Disable OTel SDK shutdown hook registration
  ([#1022](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/1022))

### Telemetry processors - New 🌟

This module contains tools for globally processing telemetry, including modifying and filtering
telemetry.

## Version 1.29.0 (2023-08-23)

### Consistent sampling

- Add a provider for consistent parent based probability sampler
  ([#1005](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/1005))

### Disk buffering

- Migrate StorageFile to FileOperations
  ([#986](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/986))

### JMX metrics

- [jmx-metrics] Collect in callback
  ([#949](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/949))
- Added transformation closure to MBeanHelper
  ([#960](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/960))

## Version 1.28.0 (2023-07-14)

### AWS X-Ray SDK support

- generate error/fault metrics by aws sdk status code
  ([#924](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/924))

### Disk buffering - New 🌟

This module provides signal exporter wrappers that intercept and store telemetry signals in files
which can be sent later on demand.

## Version 1.27.0 (2023-06-16)

### AWS X-Ray SDK support

- Enhance AWS APM metrics mapping implementation
  ([#906](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/906))

### Samplers

- Links based sampler
  ([#813](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/813),
  [#903](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/903))

## Version 1.26.0 (2023-05-17)

### AWS X-Ray SDK support

- Add AttributePropagatingSpanProcessor component to AWS X-Ray
  ([#856](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/856))
- Add new components to allow for generating metrics from 100% of spans without impacting sampling
  ([#802](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/802))

### JMX metrics

- Adding support for scenarios where the RMI registry has SSL enabled
  ([#835](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/835))

## Version 1.25.1 (2023-04-21)

### 🛠️ Bug fixes

- Previously targeted OpenTelemetry SDK and Instrumentation versions had never been updated to
  target OpenTelemetry SDK 1.25

## Version 1.25.0 (2023-04-18)

### AWS X-Ray SDK support

- Breakout ResourceHolder from AwsXrayRemoteSamplerProvider
  ([#801](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/801))

### JFR connection - New 🌟

- JFR connection is a library to allow configuration and control of JFR
  without depending on jdk.jfr.
  This is a contribution of https://github.com/microsoft/jfr-streaming.

## Version 1.24.0 (2023-04-03)

### Maven extension

- [maven-extension] Emit a warning rather than failing the build with an exception on illegal state
  ([#776](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/776))
- [maven-extension] Remove dependency to grpc-netty-shaded as opentelemetry-exporter-otlp pulls
  okhttp3
  ([#785](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/785))
- [maven-extension] Propagate OTel context to plugin mojos
  ([#786](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/786))

## Version 1.23.0 (2023-02-22)

### JFR streaming

- JFR features can be enabled and disabled
  ([#709](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/709))
- Generate JFR telemetry table
  ([#715](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/715))

### Resource providers

- Fix and enhance resource detection logging.
  ([#711](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/711))

### Samplers

- Allow providing a custom sampler as an option for the RuleBasedRoutingSampler
  ([#710](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/710))

## Version 1.22.0 (2023-01-17)

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
