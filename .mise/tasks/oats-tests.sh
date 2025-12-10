#!/usr/bin/env bash
#MISE description="Run OATS tests for cel-sampler"

set -euo pipefail

echo "==> Building cel-sampler shadow JAR..."
./gradlew :cel-sampler:shadowJar

echo "==> Building test application..."
# testapp is a standalone Gradle project, needs to be built separately
(cd cel-sampler/testapp && ../../gradlew jar)

echo "==> Running OATS integration tests..."
# Use full path to oats binary or ensure it's in PATH
OATS="${OATS:-${HOME}/go/bin/oats}"
(cd cel-sampler && "${OATS}" -timeout 5m oats/oats.yaml)
