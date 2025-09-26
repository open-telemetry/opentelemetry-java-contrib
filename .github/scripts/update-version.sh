#!/bin/bash -e

version=$1

if [[ $version == *-SNAPSHOT ]]; then
  alpha_version=${version//-SNAPSHOT/-alpha-SNAPSHOT}
else
  alpha_version=${version}-alpha
fi

sed -Ei "s/val stableVersion = \"[^\"]*\"/val stableVersion = \"$version\"/" version.gradle.kts
sed -Ei "s/val alphaVersion = \"[^\"]*\"/val alphaVersion = \"$alpha_version\"/" version.gradle.kts

sed -Ei "1 s/(Comparing source compatibility of [a-z-]+)-[0-9]+\.[0-9]+\.[0-9]+(-SNAPSHOT)?.jar/\1-$version.jar/" docs/apidiffs/current_vs_latest/*.txt
