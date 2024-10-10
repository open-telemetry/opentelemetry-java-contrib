# JMX Metric Gatherer

This utility provides an easy framework for gathering and reporting metrics based on queried
MBeans from a JMX server.  It loads included and/or custom Groovy scripts and establishes a helpful,
bound `otel` object with methods for obtaining MBeans and constructing OpenTelemetry instruments:

## Usage

The JMX Metric Gatherer is intended to be run as an uber jar and configured with properties from the command line,
properties file, and stdin (`-`).  Its metric-gathering scripts are specified by supported `otel.jmx.target.system`
values and/or a `otel.jmx.groovy.script` path to run your own.

```bash
java -D<otel.jmx.property=value> -jar opentelemetry-jmx-metrics-<version>.jar [-config {session.properties, '-'}]
```

### `session.properties`

```properties
otel.jmx.service.url = service:jmx:rmi:///jndi/rmi://<my-jmx-host>:<my-jmx-port>/jmxrmi
otel.jmx.target.system = jvm,kafka
otel.jmx.interval.milliseconds = 5000
otel.jmx.username = my-username
otel.jmx.password = my-password
otel.jmx.remote.registry.ssl=false
otel.metrics.exporter = otlp
otel.exporter.otlp.endpoint = http://my-opentelemetry-collector:4317
```

As configured in this example, the metric gatherer will establish an MBean server connection using the
specified `otel.jmx.service.url` (required) and credentials and configure an OTLP gRPC metrics exporter reporting to
`otel.exporter.otlp.endpoint`. If SSL is enabled on the RMI registry for your server, the
`otel.jmx.remote.registry.ssl` property must be set to `true`. After loading the included JVM and
Kafka metric-gathering scripts determined by the comma-separated list in `otel.jmx.target.system`,
it will then run the scripts on the desired interval length of `otel.jmx.interval.milliseconds` and
export the resulting metrics.

Some metrics (e.g. `tomcat.sessions`) are configured to query multiple MBeans. By default, only the value in the first MBean
is recorded for the metric and all other values are dropped. To aggregate the MBean values together, set the
`otel.jmx.aggregate.across.mbeans` property to `true`.

For custom metrics and unsupported targets, you can provide your own MBean querying scripts to produce
OpenTelemetry instruments:

```bash
java -Dotel.jmx.groovy.script=./script.groovy -jar opentelemetry-jmx-metrics-<version>.jar [-config {optional.properties, '-'}]
```

### `script.groovy`

```groovy
// Query the target JMX server for the desired MBean and create a helper representing the first result
def loadMBean = otel.mbean("io.example.service:type=MyType,name=Load")

// Create a LongValueCallback which will set the instrument value to the
// loadMBean's most recent `Count` attribute's long value.  The instrument will have a
// name of "my.type.load" and the specified description and unit, respectively.
otel.instrument(
        loadMBean, "my.type.load",
        "Load, in bytes, of the service of MyType",
        "By", "Count", otel.&longValueCallback
)
```

The specified `script.groovy` file will be run on the desired `otel.jmx.interval.milliseconds` (10000 by default),
resulting in an exported `my.type.load` instrument with the observed value of the desired MBean's `Count`
attribute as queried in each interval.

## Target Systems

The JMX Metric Gatherer provides built in metric producing Groovy scripts for supported target systems
capable of being specified via the `otel.jmx.target.system` property as a comma-separated list. The
currently supported target systems are:

| `otel.jmx.target.system` |
|--------------------------|
| [`jvm`](./docs/target-systems/jvm.md) |
| [`activemq`](./docs/target-systems/activemq.md) |
| [`cassandra`](./docs/target-systems/cassandra.md) |
| [`hbase`](./docs/target-systems/hbase.md) |
| [`hadoop`](./docs/target-systems/hadoop.md) |
| [`jetty`](./docs/target-systems/jetty.md) |
| [`kafka`](./docs/target-systems/kafka.md) |
| [`kafka-consumer`](./docs/target-systems/kafka-consumer.md) |
| [`kafka-producer`](./docs/target-systems/kafka-producer.md) |
| [`solr`](./docs/target-systems/solr.md) |
| [`tomcat`](./docs/target-systems/tomcat.md) |
| [`wildfly`](./docs/target-systems/wildfly.md) |

## JMX Query Helpers

- `otel.queryJmx(String objectNameStr)`
  - This method will query the connected JMX application for the given `objectNameStr`, which can
    include wildcards.  The return value will be a sorted `List<GroovyMBean>` of zero or more
    [`GroovyMBean` objects](http://docs.groovy-lang.org/latest/html/api/groovy/jmx/GroovyMBean.html),
    which are conveniently wrapped to make accessing attributes on the MBean simple.
    See <http://groovy-lang.org/jmx.html> for more information about their usage.

- `otel.queryJmx(javax.management.ObjectName objectName)`
  - This helper has the same functionality as its other signature, but takes an `ObjectName`
    instance if constructing raw names is undesired.

## JMX `MBeanHelper` and `InstrumentHelper` Access Methods

- `otel.mbean(String objectNameStr)`
  - This method will query for the given `objectNameStr` using `otel.queryJmx()` as previously described,
    but returns an `MBeanHelper` instance representing the alphabetically first matching MBean for usage by
    subsequent `InstrumentHelper` instances (available via `otel.instrument()`) as described below.  It is
    intended to be used in cases where your `objectNameStr` will return a single element `List<GroovyMBean>`
    to avoid redundant item access.

- `otel.mbeans(String objectNameStr)`
  - This method will query for the given `objectNameStr` using `otel.queryJmx()` as previously described,
    but returns an `MBeanHelper` instance representing all matching MBeans for usage by subsequent `InstrumentHelper`
    instances (available via `otel.instrument()`) as described below.  It is intended to be used in cases
    where your given `objectNameStr` can return a multiple element `List<GroovyMBean>`.
- `otel.mbeans(List<String> objectNameStrs)`
  - This method is equivalent to the above method except, it adds support for multiple ObjectNames.
    This support is meant for when there are multiple mbeans that relate to the same metric and can be separated using labels in `otel.instrument()`.

- `otel.instrument(MBeanHelper mBeanHelper, String instrumentName, String description, String unit, Map<String, Closure> labelFuncs, String attribute, Closure instrument)`
  - This method provides the ability to easily create and automatically update instrument instances from an
    `MBeanHelper`'s underlying MBeans via an OpenTelemetry instrument helper method pointer as described below.
  - The method parameters map to those of the instrument helpers, while the additional `Map<String, Closure> labelFuncs`
    will be used to specify updated instrument labels that have access to the inspected MBean:

  ```groovy
     // This example's resulting datapoint(s) will have Labels consisting of the specified key
     // and a dynamically evaluated value from the GroovyMBean being examined.
     [ "myLabelKey": { mbean -> mbean.name().getKeyProperty("myObjectNameProperty") } ]
  ```

  - If the underlying MBean(s) held by the provided MBeanHelper are
    [`CompositeData`](https://docs.oracle.com/javase/7/docs/api/javax/management/openmbean/CompositeData.html) instances,
    each key of their `CompositeType` `keySet` will be `.`-appended to the specified `instrumentName`, whose resulting
    instrument will be updated for each respective value.
  - If the underlying MBean(s) held by the provided MBeanHelper are a mixed set of
    [`CompositeData`](https://docs.oracle.com/javase/7/docs/api/javax/management/openmbean/CompositeData.html) instances
    and simple values, the InstrumentHelper will not attempt to collect the metric. This is to prevent generating
    metrics identified with the `instrumentName` and also the `instrumentName` with the `keySet` `.`-appended,
    which breaks OpenTelemetry metric conventions.

`otel.instrument()` provides additional signatures to obtain and update the returned `InstrumentHelper`:

- `otel.instrument(MBeanHelper mBeanHelper, String name, String description, String unit, String attribute, Closure instrument)` - `labelFuncs` are empty map.
- `otel.instrument(MBeanHelper mBeanHelper, String name, String description, String attribute, Closure instrument)` - `unit` is "1" and `labelFuncs` are empty map.
- `otel.instrument(MBeanHelper mBeanHelper, String name, String attribute, Closure instrument)` - `description` is empty string, `unit` is "1" and `labelFuncs` are empty map.

In cases where you'd like to share instrument names while creating datapoints for multiple MBean attributes:

- `otel.instrument(MBeanHelper mBeanHelper, String instrumentName, String description, String unit, Map<String, Closure> labelFuncs, Map<String, Map<String, Closure>> attributeLabelFuncs, Closure instrument)`

- An example of this in Tomcat is to consolidate different thread types into one `"tomcat.threads"` metric using both `currentThreadCount` and `currentThreadsBusy` MBean attributes, labeling with their applicable `"Thread Type"`:

  ```groovy
    otel.instrument(otel.mbean("Catalina:type=ThreadPool,name=*"), "tomcat.threads", "description", "1",
    ["proto_handler" : { mbean -> mbean.name().getKeyProperty("name") }],
    ["currentThreadCount": ["Thread Type": {mbean -> "current"}],
    "currentThreadsBusy": ["Thread Type": {mbean -> "busy"}]],
    otel.&doubleValueObserver)
  ```

`otel.instrument()` provides additional signatures to allow this more expressive MBean attribute access:

- `otel.instrument(MBeanHelper mBeanHelper, String name, String description, String unit, Map<String, Map<String, Closure>> attributeLabelFuncs, Closure instrument)` - `labelFuncs` are empty map.
- `otel.instrument(MBeanHelper mBeanHelper, String name, String description, Map<String, Map<String, Closure>> attributeLabelFuncs, Closure instrument)` - `unit` is "1" and `labelFuncs` are empty map.
- `otel.instrument(MBeanHelper mBeanHelper, String name, Map<String, Map<String, Closure>> attributeLabelFuncs, Closure instrument)` - `description` is empty string, `unit` is "1" and `labelFuncs` are empty map

### MBeans with non-numeric attributes

In cases where you'd like to create metrics based on non-numeric MBean attributes, the mbean helper methods provide the ability to pass a map of closures, to transform the original extracted attribute into one that can be consumed by the instrument callbacks.

- `otel.mbean(String objectNameStr, Map<String,Closure<?>> attributeTransformation)`

- `otel.mbeans(String objectNameStr, Map<String,Closure<?>> attributeTransformation)`

- `otel.mbeans(List<String> objectNameStrs, Map<String,Closure<?>> attributeTransformation)`

These methods provide the ability to easily convert the attributes you will be extracting from the mbeans, at the time of creation for the MBeanHelper.

  ```groovy
     // In this example a String based health attribute is converted to a numeric binary value
    def someBean = otel.mbean(
        "SomeMBean", ["CustomAttrFromString": { mbean -> mbean.getProperty("Attribute") == "running" ? 1 : 0 }]
    )
    otel.instrument(someBean, "my-metric", "CustomAttrFromString", otel.&longUpDownCounterCallback)
  ```

## OpenTelemetry Synchronous Instrument Helpers

- `otel.doubleCounter(String name, String description, String unit)`

- `otel.longCounter(String name, String description, String unit)`

- `otel.doubleUpDownCounter(String name, String description, String unit)`

- `otel.longUpDownCounter(String name, String description, String unit)`

- `otel.doubleHistogram(String name, String description, String unit)`

- `otel.longHistogram(String name, String description, String unit)`

These methods will return a new or previously registered instance of the applicable metric
instruments.  Each one provides three additional signatures where unit and description
aren't desired upon invocation.

- `otel.<meterMethod>(String name, String description)` - `unit` is "1".

- `otel.<meterMethod>(String name)` - `description` is empty string and `unit` is "1".

## OpenTelemetry Asynchronous Instrument Helpers

- `otel.doubleCounterCallback(String name, String description, String unit, Closure updater)`

- `otel.longCounterCallback(String name, String description, String unit, Closure updater)`

- `otel.doubleUpDownCounterCallback(String name, String description, String unit, Closure updater)`

- `otel.longUpDownCounterCallback(String name, String description, String unit, Closure updater)`

- `otel.doubleValueCallback(String name, String description, String unit, Closure updater)`

- `otel.longValueCallback(String name, String description, String unit, Closure updater)`

These methods will return a new or previously registered instance of the applicable metric
instruments.  Each one provides two additional signatures where unit and description aren't
desired upon invocation.

- `otel.<meterMethod>(String name, String description, Closure updater)` - `unit` is "1".

- `otel.<meterMethod>(String name, Closure updater)` - `description` is empty string and `unit` is "1".

Though asynchronous instrument callbacks are exclusively set by their builders in the OpenTelemetry API,
the JMX Metric Gatherer asynchronous instrument helpers allow using the specified updater Closure for
each instrument as run on the desired interval:

```groovy
def loadMBean = otel.mbean("io.example.service:type=MyType,name=Load")
otel.longValueCallback(
        "my.type.load", "Load, in bytes, of the service of MyType", "By",
        { measurement -> measurement.observe(storageLoadMBean.getAttribute("Count")) }
)
```

## Compatibility

This metric extension supports Java 8+, though SASL is only supported where
`com.sun.security.sasl.Provider` is available.

## Configuration

The following properties are supported via the command line or specified config properties file `(-config)`.
Those provided as command line properties take priority of those contained in a properties file.  Properties
file contents can also be provided via stdin on startup when using `-config -` as an option.

| Property | Required | Description |
| ------------- | -------- | ----------- |
| `otel.jmx.service.url` | **yes** | The service URL for the JMX RMI/JMXMP endpoint (generally of the form `service:jmx:rmi:///jndi/rmi://<host>:<port>/jmxrmi` or `service:jmx:jmxmp://<host>:<port>`).|
| `otel.jmx.groovy.script` | if not using `otel.jmx.target.system` | The path for the desired Groovy script. |
| `otel.jmx.target.system` | if not using `otel.jmx.groovy.script` | A comma-separated list of the supported target applications with built in Groovy scripts. |
| `otel.jmx.interval.milliseconds` | no | How often, in milliseconds, the Groovy script should be run. Value will also be used for `otel.metric.export.interval`, if unset, to control asynchronous updates and metric exporting. 10000 by default. |
| `otel.jmx.username` | no | Username for JMX authentication, if applicable. |
| `otel.jmx.password` | no | Password for JMX authentication, if applicable. |
| `otel.jmx.remote.profile` | no | Supported JMX remote profiles are TLS in combination with SASL profiles: SASL/PLAIN, SASL/DIGEST-MD5 and SASL/CRAM-MD5. Thus valid `jmxRemoteProfiles` values are: `SASL/PLAIN`, `SASL/DIGEST-MD5`, `SASL/CRAM-MD5`, `TLS SASL/PLAIN`, `TLS SASL/DIGEST-MD5` and `TLS SASL/CRAM-MD5`. |
| `otel.jmx.realm` | no | The realm is required by profile SASL/DIGEST-MD5. |
| `otel.metrics.exporter` | no | The type of metric exporter to use: (`otlp`, `prometheus`, `inmemory`, `logging`).  `logging` by default. |
| `otel.exporter.otlp.endpoint` | no | The otlp exporter endpoint to use, Required for `otlp`.  |
| `otel.exporter.otlp.headers` | no | Any headers to include in otlp exporter metric submissions.  Of the form `header1=value1,header2=value2` |
| `otel.exporter.otlp.timeout` | no | The otlp exporter request timeout (in milliseconds).  Default is 1000.  |
| `otel.exporter.prometheus.host` | no | The prometheus collector server host. Default is `0.0.0.0`.  |
| `otel.exporter.prometheus.port` | no | The prometheus collector server port. Default is `9464`.  |
| `javax.net.ssl.keyStore` | no | The key store path is required if client authentication is enabled on the target JVM. |
| `javax.net.ssl.keyStorePassword` | no | The key store file password if required. |
| `javax.net.ssl.keyStoreType` | no | The key store type. |
| `javax.net.ssl.trustStore` | no | The trusted store path if the TLS profile is required. |
| `javax.net.ssl.trustStorePassword` | no | The trust store file password if required. |

## Component owners

- [Jason Plumb](https://github.com/breedx-splk), Splunk

## Emeritus owners

- [Miguel Rodriguez](https://github.com/Mrod1598), ObservIQ
- [Ryan Fitzpatrick](https://github.com/rmfitzpatrick), Splunk
- [Sam DeHaan](https://github.com/dehaansa), ObservIQ

Learn more about component owners in [component_owners.yml](../.github/component_owners.yml).
