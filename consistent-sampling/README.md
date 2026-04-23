# Consistent sampling

The current version of the specification for consistent probability sampling is described by
<https://github.com/open-telemetry/oteps/blob/main/text/trace/0235-sampling-threshold-in-trace-state.md>.
It uses **56** bits for representing _rejection threshold_, which corresponds to a much wider range of sampling probabilities than the original proposal.

The implementation is contained by the package `io/opentelemetry/contrib/sampler/consistent` in this repository.
The OpenTelemetry SDK incubator module provides the composable-samplers building blocks
(`ComposableSampler`, `CompositeSampler`, and the `alwaysOn`/`alwaysOff`/`probability`/
`parentThreshold`/`ruleBasedBuilder`/`annotating` factories) in
`opentelemetry-sdk-extension-incubator` under
`io.opentelemetry.sdk.extension.incubator.trace.samplers`. This module adds the following
contrib-only APIs:

* **ConsistentSampler**:
  convenience factory for the contrib-only samplers below; for the common samplers use
  `ComposableSampler`'s static factories directly
* **ConsistentAnyOf**:
  allows combining several composable samplers; it samples when at least one of them would sample
* **ConsistentRateLimitingSampler**:
  a rate limiting sampler based on exponential smoothing that dynamically adjusts the sampling
  probability based on the estimated rate of spans occurring to satisfy a given rate of sampled spans
* **ConsistentVariableThresholdSampler**:
  consistent probability sampler whose sampling probability can be updated at runtime

## Component owners

- [Otmar Ertl](https://github.com/oertl), Dynatrace
- [Peter Findeisen](https://github.com/PeterF778), Cisco

Learn more about component owners in [component_owners.yml](../.github/component_owners.yml).
