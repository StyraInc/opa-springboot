#!/bin/sh

# This script checks weather the most recent version in CHANGELOG.md contains
# the string `(unrelease)`. It is used to decide weather or not to proceed with
# release related activities in GitHub Actions.

cd "$(dirname "$0")/.."

# match '## 0.0.9 (unreleased)'
awk '$1 == "##" && $0 ~ /unreleased/ { r=1 } END { print (r==1 ? "release=false" : "release=true") }' < CHANGELOG.md
