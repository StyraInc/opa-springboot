package com.styra.opa.springboot;

import static java.util.Map.entry;

import com.fasterxml.jackson.core.type.TypeReference;
import com.styra.opa.OPAClient;
import com.styra.opa.OPAException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;

@Component
public class OPAAuthorizationManager
    implements AuthorizationManager<RequestAuthorizationContext> {

    private static final Logger logger = LoggerFactory.getLogger(
        OPAAuthorizationManager.class
    );

    private static final String SubjectType = "java_authentication";
    private static final String RequestResourceType = "endpoint";
    private static final String RequestContextType = "http";

    // If opaPath is null, then we assume the user wants to use the default path.
    private String opaPath;

    private OPAClient opa;

    public OPAAuthorizationManager() {
        String opaURL = "http://localhost:8181";
        String opaURLEnv = System.getenv("OPA_URL");
        if (opaURLEnv != null) {
            opaURL = opaURLEnv;
        }
        OPAClient opac = new OPAClient(opaURL);
        this.opa = opac;
        this.opaPath = null;
    }

    public OPAAuthorizationManager(OPAClient opa) {
        this.opa = opa;
        this.opaPath = null;
    }

    public OPAAuthorizationManager(OPAClient opa, String newOpaPath) {
        this.opa = opa;
        this.opaPath = newOpaPath;
    }

    private Map<String, Object> makeRequestInput(
        Supplier<Authentication> authentication,
        RequestAuthorizationContext object
    ) {
        Object subjectId = authentication.get().getPrincipal();
        Object subjectDetails = authentication.get().getDetails();
        Collection<? extends GrantedAuthority> subjectAuthorities =
            authentication.get().getAuthorities();

        HttpServletRequest request = object.getRequest();
        String resourceId = request.getServletPath();

        String actionName = request.getMethod();
        String actionProtocol = request.getProtocol();
        Enumeration<String> headerNamesEnumeration = request.getHeaderNames();
        HashMap<String, String> headers = new HashMap<String, String>();
        while (headerNamesEnumeration.hasMoreElements()) {
            String headerName = headerNamesEnumeration.nextElement();
            String headerValue = request.getHeader(headerName);
            if (headerValue == null) {
                continue;
            }
            headers.put(headerName, headerValue);
        }

        String contextRemoteAddr = request.getRemoteAddr();
        String contextRemoteHost = request.getRemoteHost();
        Integer contextRemotePort = request.getRemotePort();

        java.util.Map<String, Object> iMap = java.util.Map.ofEntries(
            entry(
                "subject",
                java.util.Map.ofEntries(
                    entry("type", SubjectType),
                    entry("id", subjectId),
                    entry("details", subjectDetails),
                    entry("authorities", subjectAuthorities)
                )
            ),
            entry(
                "resource",
                java.util.Map.ofEntries(
                    entry("type", RequestResourceType),
                    entry("id", resourceId)
                )
            ),
            entry(
                "action",
                java.util.Map.ofEntries(
                    entry("name", actionName),
                    entry("protocol", actionProtocol),
                    entry("headers", headers)
                )
            ),
            entry(
                "context",
                java.util.Map.ofEntries(
                    entry("type", RequestContextType),
                    entry("host", contextRemoteHost),
                    entry("ip", contextRemoteAddr),
                    entry("port", contextRemotePort)
                )
            )
        );

        return iMap;
    }

    private OPAResponse opaMachinery(
        Supplier<Authentication> authentication,
        RequestAuthorizationContext object
    ) {
        Map<String, Object> iMap = makeRequestInput(authentication, object);
        logger.trace("OPA input for request: {}", iMap);
        OPAResponse resp = null;
        try {
            if (this.opaPath != null) {
                logger.trace("OPA path is {}", this.opaPath);
                resp = opa.evaluate(
                    this.opaPath,
                    iMap,
                    new TypeReference<OPAResponse>() {}
                );
            } else {
                logger.trace("Using default OPA path");
                resp = opa.evaluate(iMap, new TypeReference<OPAResponse>() {});
            }
            logger.trace("OPA response is: {}", resp);
        } catch (OPAException e) {
            logger.error("caught exception from OPA client: {}", e);
            return null;
        }
        return resp;
    }

    public AuthorizationDecision check(
        Supplier<Authentication> authentication,
        RequestAuthorizationContext object
    ) {
        OPAResponse resp = this.opaMachinery(authentication, object);
        if (resp == null) {
            logger.trace(
                "OPA provided a null response, default-denying access"
            );
            return new AuthorizationDecision(false);
        }
        return new AuthorizationDecision(resp.getDecision());
    }

    public void verify(
        Supplier<Authentication> authentication,
        RequestAuthorizationContext object
    ) {
        OPAResponse resp = this.opaMachinery(authentication, object);
        if (resp == null) {
            throw new AccessDeniedException("null response from policy");
        }

        boolean allow = resp.getDecision();
        String reason = "access denied by policy";
        if (resp.getContext() != null) {
            reason = resp.getContext().getReason();
        }

        if (allow) {
            logger.trace(
                "access verified successfully"
            );
            return;
        }

        throw new AccessDeniedException(reason);
    }
}
