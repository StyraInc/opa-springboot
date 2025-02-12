package system.authz

default allow := false

allow if {
	input.identity == ["secret", "supersecret", "superdupersecret"][_]
}

allow if {
	input.path == ["health"]
}
