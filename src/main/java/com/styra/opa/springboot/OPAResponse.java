package com.styra.opa.springboot;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * This class models the data to be returned from an OPA-SpringBoot policy. The
 * structure of the response is defined by the AuthZEN spec
 * (https://openid.github.io/authzen).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OPAResponse {

    private boolean decision;

    private OPAResponseContext context;

    public boolean getDecision() {
        return this.decision;
    }

    public void setDecision(boolean newDecision) {
        this.decision = newDecision;
    }

    public OPAResponseContext getContext() {
        return this.context;
    }

    public void setContext(OPAResponseContext newContext) {
        this.context = newContext;
    }

    /**
     * Wraps OPAResponseContext.getReasonForDecision(). If the context is
     * omitted (which the spec permits), then it returns null.
     */
    public String getReasonForDecision(String searchKey) {
        if (this.context == null) {
            return null;
        }

        return this.context.getReasonForDecision(searchKey);
    }

}
