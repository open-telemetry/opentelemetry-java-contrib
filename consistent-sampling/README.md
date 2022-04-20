# Consistent sampling

This component adds various Sampler implementations for consistent sampling as defined by
https://github.com/open-telemetry/opentelemetry-specification/blob/main/specification/trace/tracestate-probability-sampling.md
and https://github.com/open-telemetry/opentelemetry-specification/pull/2047.

* **ConsistentSampler**:
  abstract base class of all consistent sampler implementations below
* **ConsistentAlwaysOffSampler**:
  see https://github.com/open-telemetry/opentelemetry-specification/blob/main/specification/trace/tracestate-probability-sampling.md#always-off-sampler
* **ConsistentAlwaysOnSampler**:
  see https://github.com/open-telemetry/opentelemetry-specification/blob/main/specification/trace/tracestate-probability-sampling.md#always-on-consistent-probability-sampler
* **ConsistentComposedAndSampler**:
  allows combining two consistent samplers and samples when both samplers would sample
* **ConsistentComposedOrSampler**:
  allows combining two consistent sampler and samples when at least one of both samplers would sample,
  see https://github.com/open-telemetry/opentelemetry-specification/blob/main/specification/trace/tracestate-probability-sampling.md#requirement-combine-multiple-consistent-probability-samplers-using-the-minimum-p-value
* **ConsistentParentBasedSampler**:
  see https://github.com/open-telemetry/opentelemetry-specification/blob/main/specification/trace/tracestate-probability-sampling.md#parentconsistentprobabilitybased-sampler
* **ConsistentProbabilityBasedSampler**:
  see https://github.com/open-telemetry/opentelemetry-specification/blob/main/specification/trace/tracestate-probability-sampling.md#consistentprobabilitybased-sampler
* **ConsistentRateLimitingSampler**:
  a rate limiting sampler based on exponential smoothing that dynamically adjusts the sampling
  probability based on the estimated rate of spans occurring to satisfy a given rate of sampled spans

## Component owners

- [Otmar Ertl](https://github.com/oertl), Dynatrace

Learn more about component owners in [component_owners.yml](../.github/component_owners.yml).
