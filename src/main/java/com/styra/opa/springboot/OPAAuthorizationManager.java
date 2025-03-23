package com.styra.opa.springboot;

import com.fasterxml.jackson.core.type.TypeReference;
import com.styra.opa.OPAClient;
import com.styra.opa.OPAException;
import com.styra.opa.springboot.authorization.OPAAuthorizationDecision;
import com.styra.opa.springboot.autoconfigure.OPAProperties;
import com.styra.opa.springboot.input.OPAInputActionCustomizer;
import com.styra.opa.springboot.input.OPAInputContextCustomizer;
import com.styra.opa.springboot.input.OPAInputResourceCustomizer;
import com.styra.opa.springboot.input.OPAInputSubjectCustomizer;
import com.styra.opa.springboot.input.OPAInputValidator;
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

import static com.styra.opa.springboot.input.InputConstants.ACTION;
import static com.styra.opa.springboot.input.InputConstants.ACTION_HEADERS;
import static com.styra.opa.springboot.input.InputConstants.ACTION_NAME;
import static com.styra.opa.springboot.input.InputConstants.ACTION_PROTOCOL;
import static com.styra.opa.springboot.input.InputConstants.CONTEXT;
import static com.styra.opa.springboot.input.InputConstants.CONTEXT_DATA;
import static com.styra.opa.springboot.input.InputConstants.CONTEXT_HOST;
import static com.styra.opa.springboot.input.InputConstants.CONTEXT_IP;
import static com.styra.opa.springboot.input.InputConstants.CONTEXT_PORT;
import static com.styra.opa.springboot.input.InputConstants.CONTEXT_TYPE;
import static com.styra.opa.springboot.input.InputConstants.RESOURCE;
import static com.styra.opa.springboot.input.InputConstants.RESOURCE_ID;
import static com.styra.opa.springboot.input.InputConstants.RESOURCE_TYPE;
import static com.styra.opa.springboot.input.InputConstants.SUBJECT;
import static com.styra.opa.springboot.input.InputConstants.SUBJECT_AUTHORITIES;
import static com.styra.opa.springboot.input.InputConstants.SUBJECT_DETAILS;
import static com.styra.opa.springboot.input.InputConstants.SUBJECT_ID;
import static com.styra.opa.springboot.input.InputConstants.SUBJECT_TYPE;
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
    @Autowired(required = false)
    private OPAInputSubjectCustomizer opaInputSubjectCustomizer;
    @Autowired(required = false)
    private OPAInputResourceCustomizer opaInputResourceCustomizer;
    @Autowired(required = false)
    private OPAInputActionCustomizer opaInputActionCustomizer;
    @Autowired(required = false)
    private OPAInputContextCustomizer opaInputContextCustomizer;
    @Autowired
    private OPAInputValidator opaInputValidator;

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
            return new OPAAuthorizationDecision(false, null);
        }
        return new OPAAuthorizationDecision(opaResponse.getDecision(), opaResponse);
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
                opaResponse = opaClient.evaluate(selectedOPAPath, input, new TypeReference<>() {
                });
            } else {
                LOGGER.trace("Using default OPA path");
                opaResponse = opaClient.evaluate(input, new TypeReference<>() {
                });
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
        HttpServletRequest request = object.getRequest();

        Object subjectId = null;
        Object subjectDetails = null;
        Collection<? extends GrantedAuthority> subjectAuthorities = null;
        Authentication authentication = authenticationSupplier.get();
        if (authentication != null) {
            subjectId = authentication.getPrincipal();
            subjectDetails = authentication.getDetails();
            subjectAuthorities = authentication.getAuthorities();
        }
        Map<String, Object> subject = new HashMap<>();
        nullablePut(subject, SUBJECT_TYPE, opaProperties.getRequest().getSubject().getType());
        nullablePut(subject, SUBJECT_ID, subjectId);
        nullablePut(subject, SUBJECT_DETAILS, subjectDetails);
        nullablePut(subject, SUBJECT_AUTHORITIES, subjectAuthorities);
        if (opaInputSubjectCustomizer != null) {
            subject = opaInputSubjectCustomizer.customize(authentication, object, subject);
        }

        String resourceId = request.getServletPath();
        Map<String, Object> resource = Map.ofEntries(
            entry(RESOURCE_TYPE, opaProperties.getRequest().getResource().getType()),
            entry(RESOURCE_ID, resourceId)
        );
        if (opaInputResourceCustomizer != null) {
            resource = opaInputResourceCustomizer.customize(authentication, object, resource);
        }

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
        Map<String, Object> action = Map.ofEntries(
            entry(ACTION_NAME, actionName),
            entry(ACTION_PROTOCOL, actionProtocol),
            entry(ACTION_HEADERS, actionHeaders)
        );
        if (opaInputActionCustomizer != null) {
            action = opaInputActionCustomizer.customize(authentication, object, action);
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
        if (opaInputContextCustomizer != null) {
            context = opaInputContextCustomizer.customize(authentication, object, context);
        }

        Map<String, Object> input = context != null
            ? Map.of(SUBJECT, subject, RESOURCE, resource, ACTION, action, CONTEXT, context)
            : Map.of(SUBJECT, subject, RESOURCE, resource, ACTION, action);

        Optional.ofNullable(opaInputValidator).ifPresent(
            validator -> validator.validate(authentication, object, input));

        return input;
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
