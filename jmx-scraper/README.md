# JMX Metric Scraper

This utility provides a way to query JMX metrics and export them to an OTLP endpoint.
The JMX MBeans and their metric mappings are defined in YAML and reuse implementation from
[jmx-metrics instrumentation](https://github.com/open-telemetry/opentelemetry-java-instrumentation/tree/main/instrumentation/jmx-metrics).

This is currently a work-in-progress component not ready to be used in production.
The end goal is to provide an alternative to the [JMX Gatherer](../jmx-metrics/README.md) utility.

## Component owners

- [Jason Plumb](https://github.com/breedx-splk), Splunk
- [Robert Niedziela](https://github.com/robsunday), Splunk
- [Sylvain Juge](https://github.com/sylvainjuge), Elastic

Learn more about component owners in [component_owners.yml](../.github/component_owners.yml).
