package com.styra.opa.springboot;

import org.springframework.security.authorization.AuthorizationDecision;

/**
 * This class can be used in place of Spring's AuthorizationDecision, but also
 * embeds the OPAResponse. This allows for later access to the context
 * information included in the OPA response.
 */
public class OPAAuthorizationDecision extends AuthorizationDecision {
    private OPAResponse response;

    public OPAAuthorizationDecision(OPAResponse resp) {
        super(resp.getDecision());
        this.response = resp;
    }


    @Override
    public boolean isGranted() {
        return this.response.getDecision();
    }

    @Override
    public String toString() {
        if (this.isGranted()) {
            return "OPA Authorization Decision (allowed)";
        }

        return "OPA Authorization Decision (denied)";
    }

    public OPAResponse getOPAResponse() {
        return this.response;
    }
}
