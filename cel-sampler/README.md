# CEL-Based Sampler

## Declarative configuration

The `CelBasedSampler` supports [declarative configuration](https://opentelemetry.io/docs/languages/java/configuration/#declarative-configuration).

To use:

* Add a dependency on `io.opentelemetry.contrib:opentelemetry-cel-sampler:<version>`
  * See the [extension documentation](https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/main/examples/extension/README.md#build-and-add-extensions) for how to add extensions when using the java agent.
* Follow the [instructions](https://github.com/open-telemetry/opentelemetry-java/blob/main/sdk-extensions/incubator/README.md#declarative-configuration) to configure OpenTelemetry with declarative configuration.
* Configure the `.tracer_provider.sampler` to include the `cel_based` sampler.

Support is now available for the java agent, see an [example here](https://github.com/open-telemetry/opentelemetry-java-examples/blob/main/javaagent).

## Overview

The `CelBasedSampler` uses [Common Expression Language (CEL)](https://github.com/google/cel-spec) to create advanced sampling rules based on span attributes. CEL provides a powerful, yet simple expression language that allows you to create complex matching conditions.

## Schema

Schema for `cel_based` sampler:

```yaml
# The fallback sampler to use if no expressions match.
fallback_sampler:
  always_on:
# List of CEL expressions to evaluate. Expressions are evaluated in order.
expressions:
  # The action to take when the expression evaluates to true. Must be one of: DROP, RECORD_AND_SAMPLE.
  - action: DROP
    # The CEL expression to evaluate. Must return a boolean.
    expression: attribute['url.path'].startsWith('/actuator')
  - action: RECORD_AND_SAMPLE
    expression: attribute['http.method'] == 'GET' && attribute['http.status_code'] < 400
```

## Available variables

Available variables in CEL expressions:

* `name` (string): The span name
* `spanKind` (string): The span kind (e.g., "SERVER", "CLIENT")
* `attribute` (map): A map of span attributes

## Example configuration

Example of using `cel_based` sampler as the root sampler in `parent_based` sampler configuration:

```yaml
tracer_provider:
  sampler:
    parent_based:
      root:
        cel_based:
          fallback_sampler:
            always_on:
          expressions:
            # Drop health check endpoints
            - action: DROP
              expression: spanKind == 'SERVER' && attribute['url.path'].startsWith('/health')
            # Drop actuator endpoints
            - action: DROP
              expression: spanKind == 'SERVER' && attribute['url.path'].startsWith('/actuator')
            # Sample only HTTP GET requests with successful responses
            - action: RECORD_AND_SAMPLE
              expression: spanKind == 'SERVER' && attribute['http.method'] == 'GET' && attribute['http.status_code'] < 400
            # Selectively sample based on span name
            - action: RECORD_AND_SAMPLE
              expression: name.contains('checkout') || name.contains('payment')
            # Drop spans with specific name patterns
            - action: DROP
              expression: name.matches('.*internal.*') && spanKind == 'INTERNAL'
```

## Component owners

* [Dominic Lüchinger](https://github.com/dol), SIX Group
* [Jack Berg](https://github.com/jack-berg), New Relic
* [Jason Plumb](https://github.com/breedx-splk), Splunk
* [Trask Stalnaker](https://github.com/trask), Microsoft

Learn more about component owners in [component_owners.yml](../.github/component_owners.yml).
