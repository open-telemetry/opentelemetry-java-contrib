# Consistent sampling

There are two major components included here.

## Original proposal implementation

The original specification for consistent probability sampling is defined by
<https://github.com/open-telemetry/opentelemetry-specification/blob/main/specification/trace/tracestate-probability-sampling.md>
and <https://github.com/open-telemetry/opentelemetry-specification/pull/2047>.
It supports sampling probabilities that are power of 2 (1, 1/2, 1/4, ...), and uses 8-bit `r-value` and 8-bit `p-value` in tracestate.

The implementation of this proposal is contained by the package `io/opentelemetry/contrib/sampler/consistent` in this repository and provides various Sampler implementations.

* **ConsistentSampler**:
  abstract base class of all consistent sampler implementations below
* **ConsistentAlwaysOffSampler**:
  see <https://github.com/open-telemetry/opentelemetry-specification/blob/main/specification/trace/tracestate-probability-sampling.md#always-off-sampler>
* **ConsistentAlwaysOnSampler**:
  see <https://github.com/open-telemetry/opentelemetry-specification/blob/main/specification/trace/tracestate-probability-sampling.md#always-on-consistent-probability-sampler>
* **ConsistentComposedAndSampler**:
  allows combining two consistent samplers and samples when both samplers would sample
* **ConsistentComposedOrSampler**:
  allows combining two consistent sampler and samples when at least one of both samplers would sample,
  see <https://github.com/open-telemetry/opentelemetry-specification/blob/main/specification/trace/tracestate-probability-sampling.md#requirement-combine-multiple-consistent-probability-samplers-using-the-minimum-p-value>
* **ConsistentParentBasedSampler**:
  see <https://github.com/open-telemetry/opentelemetry-specification/blob/main/specification/trace/tracestate-probability-sampling.md#parentconsistentprobabilitybased-sampler>
* **ConsistentProbabilityBasedSampler**:
  see <https://github.com/open-telemetry/opentelemetry-specification/blob/main/specification/trace/tracestate-probability-sampling.md#consistentprobabilitybased-sampler>
* **ConsistentRateLimitingSampler**:
  a rate limiting sampler based on exponential smoothing that dynamically adjusts the sampling
  probability based on the estimated rate of spans occurring to satisfy a given rate of sampled spans

## Current proposal implementation

The current version of the specification for consistent probability sampling is described by
<https://github.com/open-telemetry/oteps/blob/main/text/trace/0235-sampling-threshold-in-trace-state.md>.
It uses **56** bits for representing _rejection threshold_, which corresponds to a much wider range of sampling probabilities than the original proposal.

The implementation of the current proposal is contained by the package `io/opentelemetry/contrib/sampler/consistent56` in this repository and provides implementation for a number of different Samplers.

* **ConsistentSampler**
  abstract base class for all consistent sampler implementations
* **ComposableSampler**:
  interface used to build hierarchies of Samplers, see [Composite Samplers](https://github.com/open-telemetry/oteps/pull/250)
* **ConsistentAlwaysOffSampler**:
* **ConsistentAlwaysOnSampler**:
* **ConsistentAnyOfSampler**:
  allows combining several consistent samplers; it samples when at least one of them would sample,
* **ConsistentParentBasedSampler**:
* **ConsistentFixedThresholdSampler**:
  consistent probability sampler that uses a predefined sampling probability
* **ConsistentRateLimitingSampler**:
  a rate limiting sampler based on exponential smoothing that dynamically adjusts the sampling
  probability based on the estimated rate of spans occurring to satisfy a given rate of sampled spans
* **ConsistentRuleBasedSampler**
  a sampler that performs stratified sampling by evaluating qualifying conditions and propagating the sampling decision from one of its delegate samplers

## Component owners

- [Otmar Ertl](https://github.com/oertl), Dynatrace
- [Peter Findeisen](https://github.com/PeterF778), Cisco

Learn more about component owners in [component_owners.yml](../.github/component_owners.yml).
