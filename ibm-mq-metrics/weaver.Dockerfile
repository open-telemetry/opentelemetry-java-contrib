# DO NOT BUILD
# This file is just for tracking dependencies of the semantic convention build.
# Dependabot can keep this file up to date with latest containers.

# Weaver is used to generate markdown docs, and enforce policies on the model and run integration tests.
FROM otel/weaver:v0.21.2@sha256:2401de985c38bdb98b43918e2f43aa36b2afed4aa5669ac1c1de0a17301cd36d AS weaver