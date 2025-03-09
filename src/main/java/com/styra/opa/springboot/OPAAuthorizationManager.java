package com.styra.opa.springboot;

import com.fasterxml.jackson.core.type.TypeReference;
import com.styra.opa.OPAClient;
import com.styra.opa.OPAException;
import com.styra.opa.springboot.autoconfigure.OPAProperties;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.util.Optional;
import java.util.function.Supplier;

import static java.util.Map.entry;

/**
 * This class implements {@link AuthorizationManager} which wraps the
 * <a href="https://github.com/StyraInc/opa-java">OPA Java SDK</a>. Authorization will be done in
 * {@link #check(Supplier, RequestAuthorizationContext)} and {@link #verify(Supplier, RequestAuthorizationContext)} by:
 * <ol>
 *    <li>constructing an <a href="https://docs.styra.com/sdk/springboot/reference/input-output-schema#input">input</a>
 *    (map) based on {@link Authentication} and {@link RequestAuthorizationContext}</li>
 *    <li>sending an HTTP request with the input as the request body to the OPA server</li>
 *    <li>receiving the <a href="https://docs.styra.com/sdk/springboot/reference/input-output-schema#output">output</a>
 *    as an {@link OPAResponse} and using it for authorization</li>
 * </ol>
 *  OPA input (request body) and response are compliant with the
 *  <a href="https://openid.github.io/authzen">AuthZEN spec</a>.
 */
@Component
public class OPAAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {

    static final String CONTEXT_TYPE = "type";
    static final String CONTEXT_HOST = "host";
    static final String CONTEXT_IP = "ip";
    static final String CONTEXT_PORT = "port";
    static final String CONTEXT_DATA = "data";
    static final String SUBJECT_TYPE = "type";
    static final String SUBJECT_ID = "id";
    static final String SUBJECT_DETAILS = "details";
    static final String SUBJECT_AUTHORITIES = "authorities";
    static final String SUBJECT = "subject";
    static final String RESOURCE = "resource";
    static final String ACTION = "action";
    static final String RESOURCE_TYPE = "type";
    static final String RESOURCE_ID = "id";
    static final String ACTION_NAME = "name";
    static final String ACTION_PROTOCOL = "protocol";
    static final String ACTION_HEADERS = "headers";
    static final String CONTEXT = "context";
    private static final Logger LOGGER = LoggerFactory.getLogger(OPAAuthorizationManager.class);

    private final String opaPath;
    @Getter
    private String reasonKey = OPAProperties.Response.Context.DEFAULT_REASON_KEY;
    private final ContextDataProvider contextDataProvider;
    private final OPAClient opaClient;
    @Autowired
    private OPAProperties opaProperties;
    @Autowired
    private OPAPathSelector opaPathSelector;

    public OPAAuthorizationManager() {
        this(null, null, null);
    }

    /**
     * @see OPAAuthorizationManager#OPAAuthorizationManager(OPAClient, String, ContextDataProvider)
     */
    public OPAAuthorizationManager(OPAClient opaClient) {
        this(opaClient, null, null);
    }

    /**
     * @see OPAAuthorizationManager#OPAAuthorizationManager(OPAClient, String, ContextDataProvider)
     */
    public OPAAuthorizationManager(String opaPath) {
        this(null, opaPath, null);
    }

    /**
     * @see OPAAuthorizationManager#OPAAuthorizationManager(OPAClient, String, ContextDataProvider)
     */
    public OPAAuthorizationManager(OPAClient opaClient, String opaPath) {
        this(opaClient, opaPath, null);
    }

    /**
     * @see OPAAuthorizationManager#OPAAuthorizationManager(OPAClient, String, ContextDataProvider)
     */
    public OPAAuthorizationManager(OPAClient opaClient, ContextDataProvider contextDataProvider) {
        this(opaClient, null, contextDataProvider);
    }

    /**
     * @see OPAAuthorizationManager#OPAAuthorizationManager(OPAClient, String, ContextDataProvider)
     */
    public OPAAuthorizationManager(String opaPath, ContextDataProvider contextDataProvider) {
        this(null, opaPath, contextDataProvider);
    }

    /**
     * Instantiates an instance to authorizes requests.
     *
     * @param opaClient if null, a default {@link OPAClient} will be created using {@code OPA_URL} environment variable
     *                  or default OPA url ({@value OPAProperties#DEFAULT_URL}).
     * @param opaPath if null, the default path defined by the OPA configuration will be used, unless an
     * {@link OPAPathSelector} bean is defined.
     * @param contextDataProvider helps providing additional context data in {@code input.context.data}.
     */
    public OPAAuthorizationManager(OPAClient opaClient, String opaPath, ContextDataProvider contextDataProvider) {
        opaProperties = new OPAProperties();
        this.opaClient = opaClient != null ? opaClient : defaultOPAClient();
        this.opaPath = opaPath;
        this.contextDataProvider = contextDataProvider;
    }

    private static OPAClient defaultOPAClient() {
        String opaUrl = OPAProperties.DEFAULT_URL;
        String opaUrlEnv = System.getenv("OPA_URL");
        if (opaUrlEnv != null) {
            opaUrl = opaUrlEnv;
        }
        return new OPAClient(opaUrl);
    }

    @Override
    public void verify(Supplier<Authentication> authenticationSupplier, RequestAuthorizationContext object) {
        OPAResponse opaResponse = opaRequest(authenticationSupplier, object);
        if (opaResponse == null) {
            throw new AccessDeniedException("null response from policy");
        }

        boolean decision = opaResponse.getDecision();
        if (decision) {
            LOGGER.trace("access verified successfully");
            return;
        }

        String reason = opaResponse.getReasonForDecision(reasonKey);
        if (reason == null) {
            reason = "access denied by policy";
        }
        throw new AccessDeniedException(reason);
    }

    @Override
    public AuthorizationDecision check(Supplier<Authentication> authenticationSupplier,
                                       RequestAuthorizationContext object) {
        OPAResponse opaResponse = opaRequest(authenticationSupplier, object);
        if (opaResponse == null) {
            LOGGER.trace("OPA provided a null response, default-denying access");
            return new AuthorizationDecision(false);
        }
        return new AuthorizationDecision(opaResponse.getDecision());
    }

    /**
     * This method can be used to directly call OPA without generating an {@link AuthorizationDecision}, which can be
     * used to examine the OPA response. You should consider using the OPA Java SDK (which this library depends on)
     * directly rather than using this method, as it should not be needed during normal use.
     */
    public OPAResponse opaRequest(Supplier<Authentication> authenticationSupplier, RequestAuthorizationContext object) {
        Map<String, Object> input = makeRequestInput(authenticationSupplier, object);
        LOGGER.trace("OPA input (request body) is: {}", input);
        try {
            OPAResponse opaResponse;
            String selectedOPAPath = opaPathSelector != null
                    ? opaPathSelector.selectPath(authenticationSupplier.get(), object, input) : opaPath;
            if (selectedOPAPath != null) {
                LOGGER.trace("OPA path is: {}", selectedOPAPath);
                opaResponse = opaClient.evaluate(selectedOPAPath, input, new TypeReference<>() {});
            } else {
                LOGGER.trace("Using default OPA path");
                opaResponse = opaClient.evaluate(input, new TypeReference<>() {});
            }
            LOGGER.trace("OPA response is: {}", opaResponse);
            return opaResponse;
        } catch (OPAException e) {
            LOGGER.error("caught exception from OPA client:", e);
            return null;
        }
    }

    private Map<String, Object> makeRequestInput(Supplier<Authentication> authenticationSupplier,
                                                 RequestAuthorizationContext object) {
        Object subjectId = null;
        Object subjectDetails = null;
        Collection<? extends GrantedAuthority> subjectAuthorities = null;
        Authentication authentication = authenticationSupplier.get();
        if (authentication != null) {
            subjectId = authentication.getPrincipal();
            subjectDetails = authentication.getDetails();
            subjectAuthorities = authentication.getAuthorities();
        }

        HttpServletRequest request = object.getRequest();
        String resourceId = request.getServletPath();

        String actionName = request.getMethod();
        String actionProtocol = request.getProtocol();
        Enumeration<String> headerNamesEnumeration = request.getHeaderNames();
        Map<String, String> actionHeaders = new HashMap<>();
        while (headerNamesEnumeration.hasMoreElements()) {
            String headerName = headerNamesEnumeration.nextElement();
            String headerValue = request.getHeader(headerName);
            if (headerValue != null) {
                actionHeaders.put(headerName, headerValue);
            }
        }

        String contextHost = request.getRemoteHost();
        String contextIp = request.getRemoteAddr();
        Integer contextPort = request.getRemotePort();

        Map<String, Object> context = new HashMap<>();
        nullablePut(context, CONTEXT_TYPE, opaProperties.getRequest().getContext().getType());
        nullablePut(context, CONTEXT_HOST, contextHost);
        nullablePut(context, CONTEXT_IP, contextIp);
        nullablePut(context, CONTEXT_PORT, contextPort);
        if (contextDataProvider != null) {
            Object contextData = contextDataProvider.getContextData(authenticationSupplier, object);
            context.put(CONTEXT_DATA, contextData);
        }

        Map<String, Object> subject = new HashMap<>();
        nullablePut(subject, SUBJECT_TYPE, opaProperties.getRequest().getSubject().getType());
        nullablePut(subject, SUBJECT_ID, subjectId);
        nullablePut(subject, SUBJECT_DETAILS, subjectDetails);
        nullablePut(subject, SUBJECT_AUTHORITIES, subjectAuthorities);

        return Map.ofEntries(
                entry(SUBJECT, subject),
                entry(RESOURCE, Map.ofEntries(
                        entry(RESOURCE_TYPE, opaProperties.getRequest().getResource().getType()),
                        entry(RESOURCE_ID, resourceId)
                )),
                entry(ACTION, Map.ofEntries(
                        entry(ACTION_NAME, actionName),
                        entry(ACTION_PROTOCOL, actionProtocol),
                        entry(ACTION_HEADERS, actionHeaders)
                )),
                entry(CONTEXT, context)
        );
    }

    /**
     * If {@code nullableValue} is null, this function is a NO-OP, otherwise, it calls
     * {@code map}.put({@code key}, {@code nullableValue}).
     */
    private void nullablePut(Map<String, Object> map, String key, Object nullableValue) {
        Optional.ofNullable(nullableValue).ifPresent(value -> map.put(key, value));
    }

    @Autowired
    public void setOpaProperties(OPAProperties opaProperties) {
        this.opaProperties = opaProperties;
        reasonKey = opaProperties.getResponse().getContext().getReasonKey();
    }

    /**
     * Changes the "preferred" key where the access decision reason should be searched for in the {@link OPAResponse}.
     * A default value of {@value OPAProperties.Response.Context#DEFAULT_REASON_KEY} is used. If the selected
     * key is not present in the response, the key which sorts lexicographically first is used instead.
     */
    public void setReasonKey(String reasonKey) {
        this.reasonKey = reasonKey;
    }
}
