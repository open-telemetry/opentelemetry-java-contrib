#!/usr/bin/env bash
#MISE description="Lint links in modified files"

set -e

#USAGE flag "--base <base>" help="base branch to compare against (default: origin/main)" default="origin/main"
#USAGE flag "--head <head>" help="head branch to compare against (empty for local changes) (default: empty)" default=""
#USAGE flag "--event <event>" help="event name (default: pull_request)" default="pull_request"

if [ "$usage_head" = "''" ]; then
  usage_head=""
fi

# Check if lychee config was modified
# shellcheck disable=SC2086
# - because usage_head may be empty
config_modified=$(git diff --name-only --merge-base "$usage_base" $usage_head \
                  | grep -E '^(\.github/config/lychee\.toml|\.mise/tasks/lint/.*|mise\.toml)$' || true)

if [ "$usage_event" != "pull_request" ] ; then
  echo "Not a PR - checking all files."
  mise run lint:links
elif [ -n "$config_modified" ] ; then
  echo "config changes, checking all files."
  mise run lint:links
else
  # Using lychee's default extension filter here to match when it runs against all files
  # Note: --diff-filter=d filters out deleted files
  # shellcheck disable=SC2086
  # - because usage_head may be empty
  modified_files=$(git diff --name-only --diff-filter=d "$usage_base" $usage_head \
                    | grep -E '\.(md|mkd|mdx|mdown|mdwn|mkdn|mkdown|markdown|html|htm|txt)$' \
                    | tr '\n' ' ' || true)

  if [ -z "$modified_files" ]; then
    echo "No modified files, skipping link linting."
    exit 0
  fi

  # shellcheck disable=SC2086
  mise run lint:links $modified_files
fi

