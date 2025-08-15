#!/bin/bash

set -eu

SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )
java -cp "$SCRIPT_DIR/../build/libs/opentelemetry-ibm-mq-metrics-1.49.0-alpha-SNAPSHOT-all.jar:$SCRIPT_DIR/../build/libs/com.ibm.mq.allclient.jar" \
  -Dotel.logs.exporter=none -Dotel.traces.exporter=none \
  io.opentelemetry.ibm.mq.opentelemetry.Main "$SCRIPT_DIR/config.yml"
