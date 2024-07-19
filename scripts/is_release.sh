#!/bin/sh

# This script checks weather the most recent version in CHANGELOG.md contains
# the string `(unrelease)`. It is used to decide weather or not to proceed with
# release related activities in GitHub Actions.

cd "$(dirname "$0")/.."
set -e
set -u

if ! awk '$1 == "##" && $0 ~ /unreleased/ {exit(1)}' < CHANGELOG.md ; then
	echo "repository contains unreleased changes" 1>&2
	exit 1
fi
