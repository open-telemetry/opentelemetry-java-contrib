#!/bin/bash -e

version=$1

alpha_version=${version}-alpha

sed -Ei "s/val otelVersion = \"[^\"]*\"/val otelVersion = \"$version\"/" dependencyManagement/build.gradle.kts
