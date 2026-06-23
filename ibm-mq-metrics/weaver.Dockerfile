# DO NOT BUILD
# This file is just for tracking dependencies of the semantic convention build.
# Dependabot can keep this file up to date with latest containers.

# Weaver is used to generate markdown docs, and enforce policies on the model and run integration tests.
FROM otel/weaver:v0.24.1@sha256:263964a7d444e77812f7a2d654e17683c4760a968c91278acdb7a44c20ccd572 AS weaver