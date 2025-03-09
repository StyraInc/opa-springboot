package com.styra.opa.springboot;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * This class models the data to be returned from an OPA Spring Boot SDK policy. The
 * <a href="https://docs.styra.com/sdk/springboot/reference/input-output-schema#output">response schema</a> is
 * compliant with the <a href="https://openid.github.io/authzen">AuthZEN spec</a>.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OPAResponse {

    private boolean decision;
    private OPAResponseContext context;

    public boolean getDecision() {
        return decision;
    }

    /**
     * Wraps {@link OPAResponseContext#getReasonForDecision(String)}. If the context is omitted (which the spec
     * permits), then it returns null.
     */
    public String getReasonForDecision(String searchKey) {
        if (this.context == null) {
            return null;
        }
        return this.context.getReasonForDecision(searchKey);
    }
}
