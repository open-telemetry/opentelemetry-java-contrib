#!/usr/bin/env bash
#MISE description="Lint links in all files"

set -e

#USAGE arg "<file>" var=#true help="files to check" default="."

# shellcheck disable=SC2154
lychee --verbose --scheme file --include-fragments "$usage_file"
