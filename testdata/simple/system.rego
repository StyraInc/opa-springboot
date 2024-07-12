package system

# This is used to exercise the default query functionality.

msg := "this is the default path"

main := x {
    x := {"msg": msg, "echo": input}
} else {
    x := {"msg": msg}
}
