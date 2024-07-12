package com.styra.opa.springboot;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * This class models the data to be returned from an OPA-SpringBoot policy. It
 * is used for deserialization.
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

}
