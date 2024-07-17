#!/bin/sh

# This script can be used to serve the MkDocs site locally. You need to have
# Gradle working (to build the javadoc component), Python (to serve the docs
# locally), and mkdocs (to generate the rest of the site) all set up before
# you run this script.

set -x
set -e
set -u
cd "$(dirname "$0")/.."

TEMP="$(mktemp -d)"
trap "rm -rf '$TEMP'" EXIT

./scripts/build_docs.sh "$TEMP"
cd "$TEMP"

python3 -m http.server
