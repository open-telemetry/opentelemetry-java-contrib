# Samplers

## Declarative configuration

The following samplers support [declarative configuration](https://opentelemetry.io/docs/languages/java/configuration/#declarative-configuration):

* `RuleBasedRoutingSampler`
* `CelBasedSampler`

To use:

* Add a dependency on `io.opentelemetry:opentelemetry-sdk-extension-incubator:<version>`
* Follow the [instructions](https://github.com/open-telemetry/opentelemetry-java/blob/main/sdk-extensions/incubator/README.md#file-configuration) to configure OpenTelemetry with declarative configuration.
* Configure the `.tracer_provider.sampler` to include the `rule_based_routing` or `cel_based` sampler.

## RuleBasedRoutingSampler

Schema for `rule_based_routing` sampler:

```yaml
# The fallback sampler to the use if the criteria is not met.
fallback_sampler:
  always_on:
# Filter to spans of this span_kind. Must be one of: SERVER, CLIENT, INTERNAL, CONSUMER, PRODUCER.
span_kind: SERVER   # only apply to server spans
# List of rules describing spans to drop. Spans are dropped if they match one of the rules.
rules:
  # The action to take when the rule is matches. Must be of: DROP, RECORD_AND_SAMPLE.
  - action: DROP
    # The span attribute to match against.
    attribute: url.path
    # The pattern to compare the span attribute to.
    pattern: /actuator.*
```

`rule_based_routing` sampler can be used anywhere a sampler is used in the configuration model. For example, the following YAML demonstrates a typical configuration, setting `rule_based_routing` sampler as the `root` sampler of `parent_based` sampler. In this configuration:

* The `parent_based` sampler samples based on the sampling status of the parent.
* Or, if there is no parent, delegates to the `rule_based_routing` sampler.
* The `rule_based_routing` sampler drops spans where `kind=SERVER` and `url.full matches /actuator.*`, else it samples and records.

```yaml
// ... the rest of the configuration file is omitted for brevity
// For more examples see: https://github.com/open-telemetry/opentelemetry-configuration/blob/main/README.md#starter-templates
tracer_provider:
  sampler:
    parent_based:
      # Configure the parent_based sampler's root sampler to be rule_based_routing sampler.
      root:
        rule_based_routing:
          # Fallback to the always_on sampler if the criteria is not met.
          fallback_sampler:
            always_on:
          # Only apply to SERVER spans.
          span_kind: SERVER
          rules:
            # Drop spans where url.path matches the regex /actuator.* (i.e. spring boot actuator endpoints).
            - action: DROP
              attribute: url.path
              pattern: /actuator.*
```

## CelBasedSampler

The `CelBasedSampler` uses [Common Expression Language (CEL)](https://github.com/google/cel-spec) to create advanced sampling rules based on span attributes. CEL provides a powerful, yet simple expression language that allows you to create complex matching conditions.

To use:

* Add a dependency on `io.opentelemetry:opentelemetry-sdk-extension-incubator:<version>`
* Follow the [instructions](https://github.com/open-telemetry/opentelemetry-java/blob/main/sdk-extensions/incubator/README.md#file-configuration) to configure OpenTelemetry with declarative configuration.
* Configure the `.tracer_provider.sampler` to include the `cel_based` sampler.

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

Available variables in CEL expressions:

* `name` (string): The span name
* `spanKind` (string): The span kind (e.g., "SERVER", "CLIENT")
* `attribute` (map): A map of span attributes

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

- [Jack Berg](https://github.com/jack-berg), New Relic
- [Trask Stalnaker](https://github.com/trask), Microsoft

Learn more about component owners in [component_owners.yml](../.github/component_owners.yml).
