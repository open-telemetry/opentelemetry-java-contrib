# JMX Metric Gatherer

This utility provides an easy framework for gathering and reporting metrics based on queried
MBeans from a JMX server.  It loads a custom Groovy script and establishes a helpful, bound `otel`
object with methods for obtaining MBeans and constructing synchronous OpenTelemetry instruments:

### Usage

```bash
$ java -D<otel.jmx.property=value> -jar io.opentelemetry.contrib.jmx-metrics-<version>-all.jar [-config {optional_config.properties, '-'}]
```

##### `optional_config.properties` example

```properties
otel.jmx.service.url = service:jmx:rmi:///jndi/rmi://<my-jmx-host>:<my-jmx-port>/jmxrmi
otel.jmx.groovy.script = /opt/script.groovy
otel.jmx.interval.milliseconds = 5000
otel.exporter = otlp
otel.otlp.endpoint = my-opentelemetry-collector:55680
otel.jmx.username = my-username
otel.jmx.password = my-password
```

##### `script.groovy` example

```groovy
import io.opentelemetry.common.Labels

def loadMatches = otel.queryJmx("org.apache.cassandra.metrics:type=Storage,name=Load")
def load = loadMatches.first()

def lvr = otel.longValueRecorder(
        "cassandra.storage.load",
        "Size, in bytes, of the on disk data size this node manages",
        "By", [myConstantLabelKey:"myConstantLabelValue"]
)

lvr.record(load.Count, Labels.of("myKey", "myVal"))
```

As configured in the example, this metric gatherer will configure an otlp gRPC metric exporter
at the `otel.otlp.endpoint` and establish an MBean server connection using the
provided `otel.jmx.service.url`. After loading the Groovy script whose path is specified
via `otel.jmx.groovy.script`, it will then run the script on the specified
`otel.jmx.interval.milliseconds` and export the resulting metrics.

### JMX Query Helpers

- `otel.queryJmx(String objectNameStr)`
   - This method will query the connected JMX application for the given `objectName`, which can
   include wildcards.  The return value will be a `List<GroovyMBean>` of zero or more
   [`GroovyMBean` objects](http://docs.groovy-lang.org/latest/html/api/groovy/jmx/GroovyMBean.html),
   which are conveniently wrapped to make accessing attributes on the MBean simple.
   See http://groovy-lang.org/jmx.html for more information about their usage.

- `otel.queryJmx(javax.management.ObjectName objectName)`
   - This helper has the same functionality as its other signature, but takes an `ObjectName`
   instance if constructing raw names is undesired.

### OpenTelemetry Synchronous Instrument Helpers

- `otel.doubleCounter(String name, String description, String unit, Map<String, String> labels)`

- `otel.longCounter(String name, String description, String unit, Map<String, String> labels)`

- `otel.doubleUpDownCounter(String name, String description, String unit, Map<String, String> labels)`

- `otel.longUpDownCounter(String name, String description, String unit, Map<String, String> labels)`

- `otel.doubleValueRecorder(String name, String description, String unit, Map<String, String> labels)`

- `otel.longValueRecorder(String name, String description, String unit, Map<String, String> labels)`

These methods will return a new or previously registered instance of the applicable metric
instruments.  Each one provides three additional signatures  where labels, unit, and description
aren't desired upon invocation.

- `otel.<meterMethod>(String name, String description, String unit)` - `labels` are empty map.

- `otel.<meterMethod>(String name, String description)` - `unit` is "1" and `labels` are empty map.

- `otel.<meterMethod>(String name)` - `description` is empty string, `unit` is "1" and `labels` are empty map.

### Compatibility

This metric extension supports Java 7+, though SASL is only supported where
`com.sun.security.sasl.Provider` is available.

### Configuration

The following properties are supported via the command line or specified config properties file `(-config)`.
Those provided as command line properties take priority of those contained in a properties file.  Properties
file contents can also be provided via stdin on startup when using `-config -` as an option.

| Property | Required | Description |
| ------------- | -------- | ----------- |
| `otel.jmx.service.url` | **yes** | The service URL for the JMX RMI/JMXMP endpoint (generally of the form `service:jmx:rmi:///jndi/rmi://<host>:<port>/jmxrmi` or `service:jmx:jmxmp://<host>:<port>`).|
| `otel.jmx.groovy.script` | **yes** | The path for the desired Groovy script. |
| `otel.jmx.interval.milliseconds` | no | How often, in milliseconds, the Groovy script should be run and its resulting metrics exported. 10000 by default. |
| `otel.exporter` | no | The type of metric exporter to use: (`otlp`, `prometheus`, `inmemory`, `logging`).  `logging` by default. |
| `otel.otlp.endpoint` | no | The otlp exporter endpoint to use, Required for `otlp`.  |
| `otel.otlp.metric.timeout` | no | The otlp exporter request timeout (in milliseconds).  Default is 1000.  |
| `otel.otlp.use.tls` | no | Whether to use TLS for otlp channel.  Setting any value evaluates to `true`. |
| `otel.otlp.metadata` | no | Any headers to include in otlp exporter metric submissions.  Of the form `'header1=value1;header2=value2'` |
| `otel.prometheus.host` | no | The prometheus collector server host. Default is `localhost`.  |
| `otel.prometheus.port` | no | The prometheus collector server port. Default is `9090`.  |
| `otel.jmx.username` | no | Username for JMX authentication, if applicable. |
| `otel.jmx.password` | no | Password for JMX authentication, if applicable. |
| `javax.net.ssl.keyStore` | no | The key store path is required if client authentication is enabled on the target JVM. |
| `javax.net.ssl.keyStorePassword` | no | The key store file password if required. |
| `javax.net.ssl.keyStoreType` | no | The key store type. |
| `javax.net.ssl.trustStore` | no | The trusted store path if the TLS profile is required. |
| `javax.net.ssl.trustStorePassword` | no | The trust store file password if required. |
| `otel.jmx.remote.profile` | no | Supported JMX remote profiles are TLS in combination with SASL profiles: SASL/PLAIN, SASL/DIGEST-MD5 and SASL/CRAM-MD5. Thus valid `jmxRemoteProfiles` values are: `SASL/PLAIN`, `SASL/DIGEST-MD5`, `SASL/CRAM-MD5`, `TLS SASL/PLAIN`, `TLS SASL/DIGEST-MD5` and `TLS SASL/CRAM-MD5`. |
| `otel.jmx.realm` | no | The realm is required by profile SASL/DIGEST-MD5. |
