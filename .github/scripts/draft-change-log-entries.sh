#!/bin/bash -e

version=$("$(dirname "$0")/get-version.sh")

if [[ $version =~ ([0-9]+)\.([0-9]+)\.0 ]]; then
  major="${BASH_REMATCH[1]}"
  minor="${BASH_REMATCH[2]}"
else
  echo "unexpected version: $version"
  exit 1
fi

if [[ $minor == 0 ]]; then
  prior_major=$((major - 1))
  prior_minor=$(sed -n "s/^## Version $prior_major\.\([0-9]\+\)\..*/\1/p" CHANGELOG.md | head -1)
  if [[ -z $prior_minor ]]; then
    # assuming this is the first release
    range=
  else
    range="v$prior_major.$prior_minor.0..HEAD"
  fi
else
  range="v$major.$((minor - 1)).0..HEAD"
fi

declare -A component_names=()
component_names["aws-resources/"]="AWS resources"
component_names["aws-xray/"]="AWS X-Ray SDK support"
component_names["aws-xray-propagator/"]="AWS X-Ray propagator"
component_names["consistent-sampling/"]="Consistent sampling"
component_names["disk-buffering/"]="Disk buffering"
component_names["jfr-connection/"]="JFR connection"
component_names["jfr-events/"]="JFR events"
component_names["jmx-metrics/"]="JMX metrics"
component_names["maven-extension/"]="Maven extension"
component_names["micrometer-meter-provider/"]="Micrometer MeterProvider"
component_names["noop-api/"]="No-op API"
component_names["processors/"]="Telemetry processors"
component_names["extended-tracer/"]="Extended Tracer"
component_names["prometheus-client-bridge/"]="Prometheus client bridge"
component_names["runtime-attach/"]="Runtime attach"
component_names["resource-providers/"]="Resource providers"
component_names["samplers/"]="Samplers"
component_names["static-instrumenter/"]="Static instrumenter"

echo "## Unreleased"
echo

for component in */ ; do
  component_name=${component_names[$component]:=$component}
  commits=$(git log --reverse \
          --perl-regexp \
          --author='^(?!renovate\[bot\] )' \
          --pretty=format:"- %s" \
          "$range" \
          "$component")
  if [[ "$commits" != "" ]]; then
    echo "### $component_name"
    echo
    echo "$commits" \
      | sed -E 's,\(#([0-9]+)\)$,\n  ([#\1](https://github.com/open-telemetry/opentelemetry-java-contrib/pull/\1)),'
    echo
  fi
done
