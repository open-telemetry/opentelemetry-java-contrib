# DO NOT BUILD
# This file is just for tracking dependencies of the semantic convention build.
# Dependabot can keep this file up to date with latest containers.

# Weaver is used to generate markdown docs, and enforce policies on the model and run integration tests.
FROM otel/weaver:v0.22.1@sha256:33ae522ae4b71c1c562563c1d81f46aa0f79f088a0873199143a1f11ac30e5c9 AS weaver