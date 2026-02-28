#!/bin/bash

set -eux

SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )
jar=$(find "$SCRIPT_DIR/../build/libs" -name "opentelemetry-ibm-mq-metrics-*")
java -cp "$jar:$SCRIPT_DIR/../build/libs/com.ibm.mq.allclient.jar" \
  -Dotel.logs.exporter=none -Dotel.traces.exporter=none \
  io.opentelemetry.ibm.mq.opentelemetry.Main "$SCRIPT_DIR/config.yml"
