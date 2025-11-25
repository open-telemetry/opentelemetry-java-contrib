# DO NOT BUILD
# This file is just for tracking dependencies of the semantic convention build.
# Dependabot can keep this file up to date with latest containers.

# Weaver is used to generate markdown docs, and enforce policies on the model and run integration tests.
FROM otel/weaver:v0.19.0@sha256:3d20814cef548f1d31f27f054fb4cd6a05125641a9f7cc29fc7eb234e8052cd9 AS weaver