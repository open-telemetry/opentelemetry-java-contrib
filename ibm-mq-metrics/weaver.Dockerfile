# DO NOT BUILD
# This file is just for tracking dependencies of the semantic convention build.
# Dependabot can keep this file up to date with latest containers.

# Weaver is used to generate markdown docs, and enforce policies on the model and run integration tests.
FROM otel/weaver:v0.26.1@sha256:9094862c0ab261bdbcb079bb981f9a573b3659b130a6d2ab8616eca6ba37aaec AS weaver