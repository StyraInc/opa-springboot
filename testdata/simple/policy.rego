package policy

echo := {
    "decision": true,
    "context": {
        "id": "0",
        "reason_user": {
            "en": "echo rule always allows",
            "other": "other reason key",
        },
        "data": input,
    }
}

always_false := false

always_true := true

decision_always_false := {"decision": false}

decision_always_true := {"decision": true}
