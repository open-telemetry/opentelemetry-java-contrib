# Dynamic Control

[![Maven](https://img.shields.io/maven-central/v/io.opentelemetry.contrib/opentelemetry-dynamic-control?label=Maven&color=orange)](https://central.sonatype.com/artifact/io.opentelemetry.contrib/opentelemetry-dynamic-control)

Adding dynamic control of some specific features of the Java agent.

> [!WARNING]
> This is an incubating feature. Breaking changes can happen on a new release without previous
> notice and without backward compatibility guarantees.

Current plans and progress is tracked in this [meta issue](https://github.com/open-telemetry/opentelemetry-java-contrib/issues/2416)

## Using this project

This project has now reached a stage where it is usable but subject to change in any part including the API.

## Telemetry Policy

The dynamic control is implemented using [Telemetry Policy](https://github.com/open-telemetry/opentelemetry-specification/pull/4738) prototype.
An abstract outline of dynamic control using telemetry policies is that there is a flow consisting of

```text
Message -> Provider -> Policy -> Policy aggregator -> Implementer

```

A concrete example helps to understand this flow:

1. Message: A message is added (to OpAMP data structure) to specify a change to the trace "sampling rate"
2. Provider: The OpampPolicyProvider reads the message from the OpAMP data structure received via OpAMP protocol
3. Policy: The message is converted into a TraceSamplingRatePolicy, ie the policy that changes the sampling rate,
with the target "traceid rate"
4. Policy aggregator: The policy is combined with any other policy changes already applied or pending,
handling source priority and potential merges of policies
5. Implementer: TraceSamplingRatePolicyImplementer takes the policy with the new traceid sampling rate
and applies it to the sampler

## Quick Unix example

The following would test an agent-enabled app configured to use OpAMP to receive messages
that would dynamically change the trace sampling rate:

```bash
git clone https://github.com/open-telemetry/opentelemetry-java-contrib.git
cd opentelemetry-java-contrib
./gradlew dynamic-control:shadowJar
cp dynamic-control/build/libs/opentelemetry-dynamic-control-*-alpha-SNAPSHOT-all.jar ./opentelemetry-dynamic-control-all.jar
echo "sources:" > config.yaml
echo "  - kind: opamp" >> config.yaml
echo "    format: jsonkeyvalue" >> config.yaml
echo "    location: vendor" >> config.yaml
echo "    mappings:" >> config.yaml
echo "      - policyId: sampling_rate" >> config.yaml
echo "        policyType: trace-sampling" >> config.yaml
java -Dotel.javaagent.extensions=./opentelemetry-dynamic-control-all.jar -Dotel.java.experimental.telemetry.policy.init.yaml=./config.yaml -Dotel.opamp.service.url=http://127.0.0.1:4320/v1/opamp -javaagent:path/to/opentelemetry-javaagent.jar -Dotel.service.name=my-service -jar myapp.jar
```

## Building

Building consists of executing `gradlew`, and producing a jar in the
`dynamic-control/build/libs`/`dynamic-control\build\libs` directory

### Gradle execution

You can execute gradlew either from the repo root, or the project root, eg

```bash
git clone https://github.com/open-telemetry/opentelemetry-java-contrib.git
cd opentelemetry-java-contrib
#/REM Unix or windows, execute the gradlew from this directory
./gradlew dynamic-control:<TARGET>
.\gradlew dynamic-control:<TARGET>
```

or

```bash
git clone https://github.com/open-telemetry/opentelemetry-java-contrib.git
cd opentelemetry-java-contrib
cd dynamic-control
#/REM Unix or windows, execute the gradlew from the directory above
../gradlew <TARGET>
..\gradlew <TARGET>
```

Useful values for `<TARGET>` are `jar` for the jar containing the classes from this project,
and `shadowJar` to create a jar containing the classes from this project plus all dependencies.
The latter target will produce a `*-all.jar` jar. The extension is loaded in a separate classloader
so there are unlikely to be classloading conflicts, but it's not impossible. The `jar` target
requires you to ensure you have all required dependencies in the JVM classpath. Using the `shadowJar`
is usually easier and more useful, but has a larger jar size and may bring in classes you won't need.
I'd start with `shadowJar` then switch to `jar` and explicit dependency management if that's needed.

## Using as an extension

The jar from the build needs to be accessible by the agent during startup.
Assume you have put the jar at /path/to/jar (or \path\to\jar) then you need to use
the option `otel.javaagent.extensions` (or OTEL_JAVAAGENT_EXTENSIONS environment variable) to specify where the jar is,
eg `java -Dotel.javaagent.extensions=/path/to/jar ...` when starting the application with the agent,
for the agent to include the extension.

Simply including the extension will not enable anything by default. You also need to configure the extension.
The next sections explain that.
First the general config idea is covered, then a more detailed explanation of the config that can be used.

### Using as a declarative config extension

The declarative config file should include a top level `telemetry_policy/development` node which then
uses the config defined below starting at `sources` for its content config, eg

```yaml
telemetry_policy/development:
  sources:
    - kind: opamp
      format: jsonkeyvalue
      location: vendor
      mappings:
        - policyId: sampling_rate
          policyType: trace-sampling

```

### Using as an auto-configured extension

You can use either `otel.java.experimental.telemetry.policy.init.yaml`
(or OTEL_JAVA_EXPERIMENTAL_TELEMETRY_POLICY_INIT_YAML environment variable)
to specify the file containing a YAML configuration of the policies,
or `otel.java.experimental.telemetry.policy.init.json`
(or OTEL_JAVA_EXPERIMENTAL_TELEMETRY_POLICY_INIT_JSON environment variable)
to specify the file containing a JSON configuration of the policies.
For example `java -Dotel.javaagent.extensions=/path/to/jar
-Dotel.java.experimental.telemetry.policy.init.yaml=/path/to/yaml ...`
where /path/to/yaml is a file containing the config. eg

```yaml
sources:
  - kind: opamp
    format: jsonkeyvalue
    location: vendor
    mappings:
      - policyId: sampling_rate
        policyType: trace-sampling

```

### Config Available

The config tree starts with `sources`. You can configure multiple sources.

Each source must specify:

* `kind`: where policy updates come from. Supported values: `opamp`, `file`, `http` and `custom`
(currently only `opamp` creates an active provider, the others are no-op providers)
  * `opamp`: the implemented OpAMP provider expects to read the OpAMP config map,
finding the value at the key given by `location`. The contents of that value are parseable by the capability given in `format`
* `format`: how the source payload is parsed. Supported values currently are `jsonkeyvalue` and `keyvalue`
  * `jsonkeyvalue`: expects the contents to be convertible as a string into a single or an array of
simple json objects that are key and value, eg '{ "key": value}' or '[{ "key1": value1}, { "key2": value2}]'
  * `keyvalue`: expects the contents to be convertible as a string into one or more
line separated 'key=value' pairs (eg a properties file)
* `mappings`: one or more mappings from policy IDs to dynamic-control policy types
* `location`: optional source-specific selector. For `opamp`, this is the OpAMP config map key, for example `vendor`

Each mapping must specify:

* `policyId`: the key in the source payload, for example `sampling_rate` (this is an arbitrary string defined
by the user or already being used/sent from some source)
* `policyType`: the dynamic-control policy type to update, currently only `trace-sampling` is valid

Currently supported values in summary

```yaml
sources:
  - kind: opamp|file|http|custom
    format: jsonkeyvalue|keyvalue
    location: 'opamp config map key'|'file path'|'http url'|omit row
    mappings:
      - policyId: user-defined-string
        policyType: trace-sampling

```

### Policies supported

* `trace-sampling`
  * IMPORTANT: if this policy is included in the config, then the sampler installed is overridden
and a consistent sampling sampler is installed
(technically the ComposableSampler.parentThreshold(ComposableSampler.probability()) sampler)
  * Expects a value between 0.0 and 1.0 (including both end values), and will apply that sampling rate
to the agent's sampler where 0.0 is 0% head sampling and 1.0 is 100% sampling

### Config example

Working through the following example may be helpful (it assumes the `file` source has been implemented,
this is not yet the case).

```yaml
sources:
  - kind: opamp
    format: jsonkeyvalue
    location: vendor
    mappings:
      - policyId: sampling_rate
        policyType: trace-sampling
  - kind: file
    format: keyvalue
    location: /path/to/here.conf
    mappings:
      - policyId: trace_rate
        policyType: trace-sampling
      - policyId: traceid_ratio
        policyType: trace-sampling

```

There are two sources. The first expects a message from an OpAMP server (`kind: opamp`),
from which it will access the config map and extract the value at key `vendor` (`location: vendor`).
The value is expected to be this json style key-value (`format: jsonkeyvalue`)
object string (ignoring whitespace and numeric diffs) {"sampling_rate": 0.5} (key defined by `policyId: sampling_rate`).
On receipt of this, the message is converted to a trace-sampling policy (`policyType: trace-sampling`)
and the new sampling rate applied to the sampler.

The second source expects a file (`kind: file`) at file path /path/to/here.conf (`location: /path/to/here.conf`)
which when changed will be re-read. The contents are expected to be key=value entries,
one per line (`format: keyvalue`). The only keys recognized are `trace_rate` (`policyId: trace_rate`)
and `traceid_ratio` (`policyId: traceid_ratio`). When the value changes, the message is converted
to a trace-sampling policy (`policyType: trace-sampling`)
and the new sampling rate applied to the sampler.

Because `opamp` source has higher priority than `file` source, if both sources generate a change
at the same time, the opamp change would be applied and the file change dropped.

## policyType, id, and name

Telemetry policy uses `policyType`, `id`, and `name` for different purposes:

* The pipeline initialization maps a policy ID (`policyId`) to a `policyType`, which selects the Java policy implementation.
  For example, `policyType: trace-sampling` tells dynamic control to validate matching source
  values as trace sampling policies and deliver them to the trace sampling implementer.
* `id` identifies one policy instance within a policy type. It is used to distinguish updates,
  removals, and duplicate policies of the same type.
* `name` is a human-readable description of the policy identified by `id`.

An example helps to understand the differences between these fields. `trace-sampling` is an existing
policy type, detailed above, and because it uses a sampling rate applied globally, the id for it
would be irrelevant. A different implementation, say called `trace-sampling-per-span`, could
allow different sampling rates to be applied to different types of spans. So you could sample
db spans at 5%, but pings to the `/health` endpoint at only 1%. In this case both policies are of
type `trace-sampling-per-span`, but one would have id `sample-database-spans` while the other
would have id `sample-healthcheck-spans`. The id would allow these two different instances to be
updated separately.

The pipeline initialization maps a policy ID to the implementation:

```yaml
sources:
  - kind: opamp
    format: jsonkeyvalue
    location: vendor
    mappings:
      - policyId: sample-database-spans
        policyType: trace-sampling-per-span
      - policyId: sample-healthcheck-spans
        policyType: trace-sampling-per-span
```

The source payload would use the policy ID as the key for this current key-value-style
format.
The resulting policies would have the same policyType but different ids
(note `matching` is defined in the spec as part of the policy not the pipeline,
but is not yet implemented in this repo,
so the `match` field here is currently for illustrative purposes only):

```json
{
  "sample-database-spans": {
    "id": "sample-database-spans",
    "name": "Sample database spans",
    "match": {
      "span_attribute": ["db.system"],
      "exists": true
    },
    "probability": 0.05
  }
}
```

```json
{
  "sample-healthcheck-spans": {
    "id": "sample-healthcheck-spans",
    "name": "Sample healthcheck spans",
    "match": {
      "span_attribute": ["http.route"],
      "exact": "/health"
    },
    "probability": 0.01
  }
}
```

These can be separate messages or both sent in one message (as a JSON array).
Because the IDs are different, these would be two different policies. It would be
the responsibility of the sender/updater to manage the IDs and matchers consistently.

## Component owners

* [Jack Shirazi](https://github.com/jackshirazi), Elastic
* [Cesar Munoz](https://github.com/LikeTheSalad), Elastic

Learn more about component owners in [component_owners.yml](../.github/component_owners.yml).
