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

cat <<'EOF' > "$TEMP/index.html"
<!DOCTYPE HTML>
<html lang="en-US">
    <head>
        <meta charset="UTF-8">
        <meta http-equiv="refresh" content="0; url=./javadoc">
        <script type="text/javascript">
            window.location.href = `${window.location.href}/javadoc`
        </script>
        <title>Page Redirection</title>
    </head>
    <body>
        <a href='./javadoc'>redirect to OPA-SpringBoot javadoc</a>.
    </body>
</html>
EOF

cd "$TEMP"

cp -R * "$OUTPUT_DIR"

