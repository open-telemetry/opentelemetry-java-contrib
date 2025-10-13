#!/usr/bin/env bash
#MISE description="Lint links in local files"

set -e

#USAGE arg "<file>" var=#true help="files to check" default="."

for f in $usage_file; do
    echo "Checking links in file: $f"
done

# shellcheck disable=SC2086
lychee --verbose --scheme file --include-fragments $usage_file
