package com.styra.opa.springboot;

import org.springframework.security.access.AccessDeniedException;

public class OPAAccessDeniedException extends AccessDeniedException {
    private OPAResponse response;

    public OPAAccessDeniedException(OPAResponse resp, String reason) {
        super(resp.getReasonForDecision("en"));
        this.response = resp;
    }

    public OPAAccessDeniedException(OPAResponse resp) {
        super(resp.getReasonForDecision("en"));
        this.response = resp;
    }

    public OPAResponse getOPAResponse() {
        return this.response;
    }
}
