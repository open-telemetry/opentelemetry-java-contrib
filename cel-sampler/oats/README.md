# CEL Sampler OATS Integration Tests

This directory contains acceptance tests for the CEL-based sampler extension using the
[OATS (OpenTelemetry Acceptance Test Suite)](https://github.com/grafana/oats) framework.

## Overview

These tests verify that the CEL sampler correctly:
- Drops traces for health check endpoints (`/healthcheck`, `/metrics`)
- Samples traces for regular API endpoints (`/hello`, `/api/data`)
- Loads correctly as a Java agent extension
- Works with declarative configuration

## Running the Tests

You can build all assets and run tests using:

`mise run oats-test`

Or manually run just the tests (from the `cel-sampler` directory):

```bash
~/go/bin/oats oats/oats.yaml
```
