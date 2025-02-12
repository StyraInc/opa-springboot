package system

import rego.v1

# This is used to exercise the default query functionality.

msg := "this is the default path"

main := x if {
    x := {"msg": msg, "echo": input}
} else if {
    x := {"msg": msg}
}
