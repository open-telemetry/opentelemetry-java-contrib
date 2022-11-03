#!/bin/bash -e

version=$1

sed -Ei "s/val otelInstrumentationVersion = \"[^\"]*\"/val otelInstrumentationVersion = \"$version\"/" dependencyManagement/build.gradle.kts
