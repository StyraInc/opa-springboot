package com.styra.opa.springboot.authorization;

import com.styra.opa.springboot.OPAResponse;
import lombok.Getter;
import org.springframework.security.authorization.AuthorizationDecision;

/**
 * Extends {@link AuthorizationDecision} which conveys {@link OPAResponse}.
 */
@Getter
public class OPAAuthorizationDecision extends AuthorizationDecision {
    private final OPAResponse opaResponse;

    public OPAAuthorizationDecision(boolean granted, OPAResponse opaResponse) {
        super(granted);
        this.opaResponse = opaResponse;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [granted=" + isGranted() + ", opaResponse=" + opaResponse + "]";
    }
}
