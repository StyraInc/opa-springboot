package com.styra.opa.springboot.authorization;

import com.styra.opa.springboot.OPAResponse;
import lombok.Getter;
import org.springframework.security.access.AccessDeniedException;

/**
 * Extends {@link AccessDeniedException} which conveys {@link OPAResponse}.
 */
@Getter
public class OPAAccessDeniedException extends AccessDeniedException {

    private OPAResponse opaResponse;

    public OPAAccessDeniedException(String message) {
        super(message);
    }

    public OPAAccessDeniedException(String message, OPAResponse opaResponse) {
        super(message);
        this.opaResponse = opaResponse;
    }

    public OPAAccessDeniedException(String message, Throwable cause, OPAResponse opaResponse) {
        super(message, cause);
        this.opaResponse = opaResponse;
    }
}
