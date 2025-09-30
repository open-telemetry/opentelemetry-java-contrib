#!/usr/bin/env bash
#MISE description="Lint links in all files"

set -e

#USAGE arg "<file>" var=#true help="files to check" default="."

lychee --verbose --config .github/config/lychee.toml $usage_file
