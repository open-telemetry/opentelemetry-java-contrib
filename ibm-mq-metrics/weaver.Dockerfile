# DO NOT BUILD
# This file is just for tracking dependencies of the semantic convention build.
# Dependabot can keep this file up to date with latest containers.

# Weaver is used to generate markdown docs, and enforce policies on the model and run integration tests.
FROM otel/weaver:v0.15.1@sha256:95c0aaa493d84ac72a1188756bd46eec1ead8e82004e7778ff5779736be8d578 AS weaver