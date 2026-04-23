# Consistent sampling

This module implements the current specification for consistent probability
sampling described by
<https://github.com/open-telemetry/oteps/blob/main/text/trace/0235-sampling-threshold-in-trace-state.md>.
It uses **56** bits for representing the _rejection threshold_, which
corresponds to a much wider range of sampling probabilities than the original
OTEP&nbsp;4673 proposal.

The reference implementation of composable samplers now lives upstream in
[`opentelemetry-java`](https://github.com/open-telemetry/opentelemetry-java)
under the `opentelemetry-sdk-extension-incubator` module, in the package
`io.opentelemetry.sdk.extension.incubator.trace.samplers`. That package provides
`ComposableSampler` (including the static factories `alwaysOn()`, `alwaysOff()`,
`probability(ratio)`, `parentThreshold(rootSampler)`, `ruleBasedBuilder()`,
`annotating(sampler, attributes)`) and `CompositeSampler.wrap(ComposableSampler)`
for turning a composable sampler into a `Sampler`.

The package `io.opentelemetry.contrib.sampler.consistent` in this repository
adds the following samplers on top of the upstream incubator:

* **ConsistentRateLimitingSampler**:
  a rate limiting sampler based on exponential smoothing that dynamically adjusts the sampling
  probability based on the estimated rate of spans occurring to satisfy a given rate of sampled spans
* **ConsistentVariableThresholdSampler**:
  a consistent probability sampler whose sampling probability can be updated at runtime
* **ConsistentAnyOf**:
  allows combining several composable samplers; it samples when at least one of them would sample
* **ConsistentReservoirSamplingSpanProcessor**:
  a `SpanProcessor` that buffers ended spans and, if more spans arrive in a
  period than the configured reservoir size, consistently down-samples the
  buffer before export

The `ConsistentSampler` class is a convenience factory for the contrib-only
samplers above. For the common samplers use `ComposableSampler`'s static
factories directly.

## Autoconfigure

This module registers the `parentbased_consistent_probability` sampler name for
the OpenTelemetry autoconfigure SDK, equivalent to
`CompositeSampler.wrap(ComposableSampler.parentThreshold(ComposableSampler.probability(arg)))`.
Use it via:

```
OTEL_TRACES_SAMPLER=parentbased_consistent_probability
OTEL_TRACES_SAMPLER_ARG=0.1
```

## Component owners

- [Otmar Ertl](https://github.com/oertl), Dynatrace
- [Peter Findeisen](https://github.com/PeterF778), Cisco

Learn more about component owners in [component_owners.yml](../.github/component_owners.yml).
