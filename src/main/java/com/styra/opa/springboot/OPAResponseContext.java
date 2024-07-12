package com.styra.opa.springboot;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * This class models the data to be returned from an OPA-SpringBoot policy
 * under the context key. It is used for deserialization.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OPAResponseContext {

    private String reason;

    public String getReason() {
        return this.reason;
    }

    public void setReason(String newReason) {
        this.reason = newReason;
    }
}
