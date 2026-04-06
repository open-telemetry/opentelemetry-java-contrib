#!/bin/bash

set -eux

SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )
jar=$(find "$SCRIPT_DIR/../build/libs" -maxdepth 1 -name "opentelemetry-ibm-mq-metrics-*-all.jar" -print -quit)
if [[ -z "$jar" ]]; then
  echo "Error: Shadow JAR not found in $SCRIPT_DIR/../build/libs (expected opentelemetry-ibm-mq-metrics-*-all.jar)" >&2
  exit 1
fi
java -cp "$jar:$SCRIPT_DIR/../build/libs/com.ibm.mq.allclient.jar" \
  -Dotel.logs.exporter=none -Dotel.traces.exporter=none \
  io.opentelemetry.ibm.mq.opentelemetry.Main "$SCRIPT_DIR/config.yml"
