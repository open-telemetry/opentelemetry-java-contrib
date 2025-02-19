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
- `otel.jmx.target.system` or `otel.jmx.config`

Configuration can be provided through:

- command line arguments:
  `java -jar scraper.jar -config otel.jmx.service.url=service:jmx:rmi:///jndi/rmi://tomcat:9010/jmxrmi otel.jmx.target.system=tomcat`.
- command line arguments JVM system properties:
  `java -Dotel.jmx.service.url=service:jmx:rmi:///jndi/rmi://tomcat:9010/jmxrmi -Dotel.jmx.target.system=tomcat -jar scraper.jar`.
- java properties file: `java -jar scraper.jar -config config.properties`.
- stdin: `java -jar scraper.jar -config -` where `otel.jmx.target.system=tomcat` and
  `otel.jmx.service.url=service:jmx:rmi:///jndi/rmi://tomcat:9010/jmxrmi` is written to stdin.
- environment variables: `OTEL_JMX_TARGET_SYSTEM=tomcat OTEL_JMX_SERVICE_URL=service:jmx:rmi:///jndi/rmi://tomcat:9010/jmxrmi java -jar scraper.jar`

SDK autoconfiguration is being used, so all the configuration options can be set using the java
properties syntax or the corresponding environment variables.

For example the `otel.jmx.service.url` option can be set with the `OTEL_JMX_SERVICE_URL` environment variable.

## Configuration reference

| config option                  | default value | description                                                                                                                               |
|--------------------------------|---------------|-------------------------------------------------------------------------------------------------------------------------------------------|
| `otel.jmx.service.url`         | -             | mandatory JMX URL to connect to the remote JVM                                                                                            |
| `otel.jmx.target.system`       | -             | comma-separated list of systems to monitor, mandatory unless `otel.jmx.config` is set                                                     |
| `otel.jmx.config`              | empty         | comma-separated list of paths to custom YAML metrics definition, mandatory when `otel.jmx.target.system` is not set                       |
| `otel.jmx.username`            | -             | user name for JMX connection, mandatory when JMX authentication is set on target JVM with`com.sun.management.jmxremote.authenticate=true` |
| `otel.jmx.password`            | -             | password for JMX connection, mandatory when JMX authentication is set on target JVM with `com.sun.management.jmxremote.authenticate=true` |
| `otel.jmx.remote.registry.ssl` | `false`       | connect to an SSL-protected registry when enabled on target JVM with `com.sun.management.jmxremote.registry.ssl=true`                     |

When both `otel.jmx.target.system` and `otel.jmx.config` configuration options are used at the same time:

- `otel.jmx.target.system` provides ready-to-use metrics and `otel.jmx.config` allows to add custom definitions.
- The metrics definitions will be the aggregation of both.
- There is no guarantee on the priority or any ability to override the definitions.

If there is a need to override existing ready-to-use metrics or to keep control on the metrics definitions, using a custom YAML definition with `otel.jmx.config` is the recommended option.

Supported values for `otel.jmx.target.system`:

| `otel.jmx.target.system` | description           |
|--------------------------|-----------------------|
| `activemq`               | Apache ActiveMQ       |
| `cassandra`              | Apache Cassandra      |
| `hbase`                  | Apache HBase          |
| `hadoop`                 | Apache Hadoop         |
| `jetty`                  | Eclipse Jetty         |
| `jvm`                    | JVM runtime metrics   |
| `kafka`                  | Apache Kafka          |
| `kafka-consumer`         | Apache Kafka consumer |
| `kafka-producer`         | Apache Kafka producer |
| `solr`                   | Apache Solr           |
| `tomcat`                 | Apache Tomcat         |
| `wildfly`                | Wildfly               |

The following SDK configuration options are also relevant

| config option                 | default value   | description                                                                                                                                                       |
|-------------------------------|-----------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `otel.metric.export.interval` | `1m` (1 minute) | metric export interval, also controls the JMX sampling interval                                                                                                   |
| `otel.metrics.exporter`       | `otlp`          | comma-separated list of metrics exporters supported values are `otlp` and `logging`, additional values might be provided through extra libraries in the classpath |

In addition to OpenTelemetry configuration, the following Java system properties can be provided
through the command-line arguments, properties file or stdin and will be propagated to the JVM system properties:

- `javax.net.ssl.keyStore`
- `javax.net.ssl.keyStorePassword`
- `javax.net.ssl.trustStore`
- `javax.net.ssl.trustStorePassword`

Those JVM system properties can't be set through individual environment variables, but they can still
be set through the standard `JAVA_TOOL_OPTIONS` environment variable using the `-D` prefix.

## Troubleshooting

### Exported metrics

In order to investigate when and what metrics are being captured and sent, setting the `otel.metrics.exporter`
configuration option to include `logging` exporter provides log messages when metrics are being exported

### JMX connection test

Connection to the remote JVM through the JMX can be tested by adding the `-test` argument.
When doing so, the JMX Scraper will only test the connection to the remote JVM with provided configuration
and exit.

- Connection OK: `JMX connection test OK` message is written to standard output and exit status = `0`
- Connection ERROR: `JMX connection test ERROR` message is written to standard output and exit status = `1`

## Extra libraries in classpath

By default, only the RMI JMX connector is provided by the JVM, so it might be required to add extra
libraries in the classpath when connecting to remote JVMs that are not directly accessible with RMI.

One known example of this is the Wildfly/Jboss HTTP management interface for which the `jboss-client.jar`
needs to be used to support `otel.jmx.service.url` = `service:jmx:remote+http://server:9999`.

When doing so, the `java -jar` command can´t be used, we have to provide the classpath with
`-cp`/`--class-path`/`-classpath` option and provide the main class file name:

```bash
java -cp scraper.jar:jboss-client.jar io.opentelemetry.contrib.jmxscraper.JmxScraper <config>
```

## Component owners

- [Jason Plumb](https://github.com/breedx-splk), Splunk
- [Robert Niedziela](https://github.com/robsunday), Splunk
- [Sylvain Juge](https://github.com/sylvainjuge), Elastic

Learn more about component owners in [component_owners.yml](../.github/component_owners.yml).
