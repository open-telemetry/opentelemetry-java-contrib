#!/bin/bash

set -e

export MSYS_NO_PATHCONV=1 # for Git Bash on Windows

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$SCRIPT_DIR/../.."
DEPENDENCIES_DOCKERFILE="$SCRIPT_DIR/dependencies.dockerfile"

# Parse command line arguments
RELATIVE_ONLY=false
MODIFIED_FILES=""

while [[ $# -gt 0 ]]; do
    case $1 in
        --relative-only)
            RELATIVE_ONLY=true
            shift
            ;;
        *)
            # Treat any other arguments as file paths
            MODIFIED_FILES="$MODIFIED_FILES $1"
            shift
            ;;
    esac
done

# Extract lychee version from dependencies.dockerfile
LYCHEE_VERSION=$(grep "FROM lycheeverse/lychee:" "$DEPENDENCIES_DOCKERFILE" | sed 's/.*FROM lycheeverse\/lychee:\([^ ]*\).*/\1/')

# Determine target files/directories and config file
TARGET="."
LYCHEE_CONFIG=".github/scripts/.lychee.toml"

if [[ "$RELATIVE_ONLY" == "true" ]]; then
    LYCHEE_CONFIG=".github/scripts/.lychee-relative.toml"
fi

if [[ -n "$MODIFIED_FILES" ]]; then
    TARGET="$MODIFIED_FILES"
fi

# Build the lychee command with optional GitHub token
CMD="lycheeverse/lychee:$LYCHEE_VERSION --verbose --config $LYCHEE_CONFIG"

# Add GitHub token if available
if [[ -n "$GITHUB_TOKEN" ]]; then
    CMD="$CMD --github-token $GITHUB_TOKEN"
fi

CMD="$CMD $TARGET"

# Determine if we should allocate a TTY
DOCKER_FLAGS="--rm --init"
if [[ -t 0 ]]; then
    DOCKER_FLAGS="$DOCKER_FLAGS -it"
else
    DOCKER_FLAGS="$DOCKER_FLAGS -i"
fi

# Run lychee with proper signal handling
# shellcheck disable=SC2086
exec docker run $DOCKER_FLAGS -v "$ROOT_DIR":/data -w /data $CMD
