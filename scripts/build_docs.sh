#!/bin/sh

# This script builds the static documentation site. You need to have Gradle and
# MkDocs set up and working on your system to run it.

set -x
set -e
set -u
cd "$(dirname "$0")/.."

if [ $# -ne 1 ] ; then
	echo "usage: $0 OUTPUT_DIR" 1>&2
	exit 1
fi

OUTPUT_DIR="$1"
if [ ! -d "$OUTPUT_DIR" ] ; then
	mkdir -p "$OUTPUT_DIR"
fi
OUTPUT_DIR="$(realpath "$OUTPUT_DIR")"

TEMP="$(mktemp -d)"
trap "rm -rf '$TEMP'" EXIT

./gradlew build javadoc -x test -x lint

mkdir "$TEMP/javadoc"
cp -R ./build/docs/javadoc/* "$TEMP/javadoc"

cd "$TEMP"

cp -R javadoc "$OUTPUT_DIR"

