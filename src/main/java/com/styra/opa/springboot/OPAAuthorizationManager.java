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


/**
 * This class implements a Spring AuthorizationManager which wraps the OPA Java
 * SDK (https://github.com/StyraInc/opa-java). OPA inputs are constructed by
 * inspecting the Spring Authentication and RequestAuthorizationContext
 * arguments to check and verify, and are compliant with the AuthZEN spec
 * (https://openid.github.io/authzen).
 */
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

    private String reasonKey;

    private ContextDataProvider ctxProvider;

    private OPAClient opa;

    /**
     * The authorization manager will internally instantiate an OPAClient
     * instance with default settings. The OPA URL may be overridden using the
     * OPA_URL environment variable. All OPA requests will be sent to the
     * default path defined by the OPA configuration.
     */
    public OPAAuthorizationManager() {
        this.opa = defaultOPAClient();
        this.opaPath = null;
        this.reasonKey = "en";
    }

    /**
     * The authorization manager will be instantiated with the provided OPA
     * client. The caller must perform any needed client configuration before
     * passing it to this constructor. The default path will be used.
     *
     * @param opa
     */
    public OPAAuthorizationManager(OPAClient opa) {
        this.opa = opa;
        this.opaPath = null;
        this.reasonKey = "en";
    }

    /**
     * The authorization manager will be instantiated with a caller-supplied
     * client, and all OPA requests will be sent to the specified path. The
     * path should be suitable for use with the check() and evaluate() methods
     * of OPAClient.
     *
     * @param opa
     * @param newOpaPath
     */
    public OPAAuthorizationManager(OPAClient opa, String newOpaPath) {
        this.opa = opa;
        this.opaPath = newOpaPath;
        this.reasonKey = "en";
    }

    /**
     * The authorization manager will internally instantiate an OPA client. The
     * OPA URL may be overridden using the OPA_URL environment variable. All
     * OPA requests will be sent to the provided path.
     *
     * @param newOpaPath
     */
    public OPAAuthorizationManager(String newOpaPath) {
        this.opa = defaultOPAClient();
        this.opaPath = newOpaPath;
        this.reasonKey = "en";
    }

    /**
     * The authorization manager will be instantiated with a caller-supplied
     * client, requests will be sent to the default path, and the caller
     * provided ContextDataProvider will be used to populate OPA input at
     * input.context.data.
     *
     * @param opa
     * @param newProvider
     */
    public OPAAuthorizationManager(OPAClient opa, ContextDataProvider newProvider) {
        this.opa = opa;
        this.ctxProvider = newProvider;
        this.reasonKey = "en";
    }

    /**
     * The authorization manager will be instantiated with a caller-supplied
     * client and path, and the ContextDataProvider will be used to populate
     * the OPA input at input.context.data.
     *
     * @param opa
     * @param newOpaPath
     * @param newProvider
     */
    public OPAAuthorizationManager(OPAClient opa, String newOpaPath, ContextDataProvider newProvider) {
        this.opa = opa;
        this.opaPath = newOpaPath;
        this.ctxProvider = newProvider;
        this.reasonKey = "en";
    }

    /**
     * The authorization manager will instantiate an OPA client internally, but
     * use a caller-supplied path, and ContextDataProvider.
     *
     * @param newOpaPath
     * @param newProvider
     */
    public OPAAuthorizationManager(String newOpaPath, ContextDataProvider newProvider) {
        this.opa = defaultOPAClient();
        this.opaPath = newOpaPath;
        this.ctxProvider = newProvider;
        this.reasonKey = "en";
    }

    private static OPAClient defaultOPAClient() {
        String opaURL = "http://localhost:8181";
        String opaURLEnv = System.getenv("OPA_URL");
        if (opaURLEnv != null) {
            opaURL = opaURLEnv;
        }
        OPAClient opac = new OPAClient(opaURL);
        return opac;
    }

    public String getReasonKey() {
        return this.reasonKey;
    }

    /**
     * Changes the "preferred" key where the access decision reason should be
     * searched for in the OPAResponse object. A default value of 'en' is used.
     * If the selected key is not present in the response, the key which sorts
     * lexicographically first is used instead.
     *
     * @param newReasonKey
     */
    public void setReasonKey(String newReasonKey) {
        this.reasonKey = newReasonKey;
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

        HashMap<String, Object> ctx = new HashMap<String, Object>();
        ctx.put("type", RequestContextType);
        ctx.put("host", contextRemoteHost);
        ctx.put("ip", contextRemoteAddr);
        ctx.put("port", contextRemotePort);

        if (this.ctxProvider != null) {
            Object contextData = this.ctxProvider.getContextData(authentication, object);
            ctx.put("data", contextData);
        }

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
            entry("context", ctx)
        );

        return iMap;
    }

    /**
     * This method can be used to directly call OPA without generating an
     * AuthorizationDecision, which can be used to examine the OPA response.
     * You should consider using the OPA Java SDK (which the OPA Spring Boot SDK depends
     * on) directly rather than using this method, as it should not be needed
     * during normal use.
     */
    public OPAResponse opaRequest(
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
        OPAResponse resp = this.opaRequest(authentication, object);
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
        OPAResponse resp = this.opaRequest(authentication, object);
        if (resp == null) {
            throw new AccessDeniedException("null response from policy");
        }

        boolean allow = resp.getDecision();
        String reason = resp.getReasonForDecision(this.reasonKey);
        if (reason == null) {
            reason = "access denied by policy";
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
