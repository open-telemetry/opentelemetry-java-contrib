#!/usr/bin/env bash
#MISE description="Lint links in local files"

set -e

#USAGE arg "<file>" var=#true help="files to check" default="."

lychee --verbose --scheme file --include-fragments "$usage_file"
