#!/bin/bash -e

for component in aws-xray samplers jfr-streaming jmx-metrics maven-extension runtime-attach; do
  echo "* $component"
  git log --reverse --pretty=format:"  * %s" "$1"..HEAD $component \
    | sed -r 's,\(#([0-9]+)\),\n    ([#\1](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/\1)),'
  echo
done
