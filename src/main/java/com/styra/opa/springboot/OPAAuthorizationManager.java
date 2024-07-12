package com.styra.opa.springboot;

import com.fasterxml.jackson.core.type.TypeReference;
import com.styra.opa.OPAClient;
import com.styra.opa.OPAException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import static java.util.Map.entry;

@Component
public class OPAAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {

    private static final Logger logger = LoggerFactory.getLogger(OPAAuthorizationManager.class);

    private static final String SubjectType = "java_authentication";
    private static final String RequestResourceType = "endpoint";
    private static final String RequestContextType = "http";

    private OPAClient opa;

    public OPAAuthorizationManager() {
        String opaURL = "http://localhost:8181";
        String opaURLEnv = System.getenv("OPA_URL");
        if (opaURLEnv != null) {
            opaURL = opaURLEnv;
        }
        OPAClient opac = new OPAClient(opaURL);
        this.opa = opac;
    }

    public OPAAuthorizationManager(OPAClient opa) {
        this.opa = opa;
    }

    private Map<String, Object> makeRequestInput(
            Supplier<Authentication> authentication,
            RequestAuthorizationContext object
    ) {
        Object subjectId = authentication.get().getPrincipal();
        Object subjectDetails = authentication.get().getDetails();
        Collection<? extends GrantedAuthority> subjectAuthorities = authentication.get().getAuthorities();

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
            entry("subject", java.util.Map.ofEntries(
                entry("type", SubjectType),
                entry("id", subjectId),
                entry("details", subjectDetails),
                entry("authorities", subjectAuthorities)
            )),
            entry("resource", java.util.Map.ofEntries(
                entry("type", RequestResourceType),
                entry("id", resourceId)
            )),
            entry("action", java.util.Map.ofEntries(
                entry("name", actionName),
                entry("protocol", actionProtocol),
                entry("headers", headers)
            )),
            entry("context", java.util.Map.ofEntries(
                entry("type", RequestContextType),
                entry("host", contextRemoteHost),
                entry("ip", contextRemoteAddr),
                entry("port", contextRemotePort)
            ))
        );

        logger.debug(String.format("created input for OPA: %s\n", iMap));

        return iMap;
    }

    private OPAResponse opaMachinery(Supplier<Authentication> authentication, RequestAuthorizationContext object) {
        Map<String, Object> iMap = makeRequestInput(authentication, object);
        OPAResponse resp = null;
        try {
            // TODO: the path needs to not be hard-coded
            resp = opa.evaluate("policy/always_true", iMap, new TypeReference<OPAResponse>() {});
            logger.debug("OPA response is: {}", resp);
        } catch (OPAException e) {
            System.out.printf("ERROR: exception from OPA client: %s\n", e);
            e.printStackTrace(System.out);
            return null;
        }
        return resp;
    }

    public AuthorizationDecision check(Supplier<Authentication> authentication, RequestAuthorizationContext object) {
        OPAResponse resp = this.opaMachinery(authentication, object);
        if (resp == null) {
            return new AuthorizationDecision(false);
        }
        return new AuthorizationDecision(resp.getDecision());
    }

    public void verify(Supplier<Authentication> authentication, RequestAuthorizationContext object) {
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
            return;
        }

        throw new AccessDeniedException(reason);
    }
}
