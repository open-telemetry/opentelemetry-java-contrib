# JMX Metric Scraper

This utility provides a way to query JMX metrics and export them to an OTLP endpoint.
The JMX MBeans and their metric mappings are defined in YAML and reuse implementation from
[jmx-metrics instrumentation](https://github.com/open-telemetry/opentelemetry-java-instrumentation/tree/main/instrumentation/jmx-metrics).

This is currently a work-in-progress component not ready to be used in production.
The end goal is to provide an alternative to the [JMX Gatherer](../jmx-metrics/README.md) utility.

## Usage

The general command to invoke JMX scraper is `java -jar scraper.jar <config>`, where `scraper.jar`
is the `build/libs/opentelemetry-jmx-scraper-<version>.jar` packaged binary when building this module.

Minimal configuration required
- `otel.jmx.service.url` for example `service:jmx:rmi:///jndi/rmi://server:9999/jmxrmi` for `server`
  host on port `9999` with RMI JMX connector.
- `otel.jmx.target.system` or `otel.jmx.custom.scraping.config`

Configuration can be provided through:
- command line arguments: `java -jar scraper.jar --config `
- system properties `java -jar scraper.jar`
- java properties file: `java -jar config.properties`

TODO: update this once autoconfiguration is supported

### Configuration reference

TODO

### Extra libraries in classpath

By default, only the RMI JMX connector is provided by the JVM, so it might be required to add extra
libraries in the classpath when connecting to remote JVMs that are not directly accessible with RMI.

One known example of this is the Wildfly/Jboss HTTP management interface for which the `jboss-client.jar`
needs to be used to support `otel.jmx.service.url` = `service:jmx:remote+http://server:9999`.

When doing so, the `java -jar` command can´t be used, we have to provide the classpath with
`-cp`/`--class-path`/`-classpath` option and provide the main class file name:
```
java -cp scraper.jar:jboss-client.jar io.opentelemetry.contrib.jmxscraper.JmxScraper <config>
```

## Component owners

- [Jason Plumb](https://github.com/breedx-splk), Splunk
- [Robert Niedziela](https://github.com/robsunday), Splunk
- [Sylvain Juge](https://github.com/sylvainjuge), Elastic

Learn more about component owners in [component_owners.yml](../.github/component_owners.yml).
