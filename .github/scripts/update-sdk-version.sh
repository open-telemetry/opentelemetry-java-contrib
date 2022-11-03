#!/bin/bash -e

version=$1

sed -Ei "s/val otelVersion = \"[^\"]*\"/val otelVersion = \"$version\"/" dependencyManagement/build.gradle.kts
