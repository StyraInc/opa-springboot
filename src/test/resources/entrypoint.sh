#!/usr/bin/env bash

set -e
set -u
set -x

# We want to run nginx attached to the same foreground session as OPA, but
# without blocking OPA. We can do this simply using Bash job control.
nginx -g "daemon off;" &

opa $@
