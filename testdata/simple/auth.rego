package system.authz

import rego.v1

default allow := false

allow if {
	input.identity == ["secret", "supersecret", "superdupersecret"][_]
}

allow if {
	input.path == ["health"]
}
