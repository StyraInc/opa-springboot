package com.styra.opa.springboot.input;

import com.styra.opa.springboot.ContextDataProvider;
import com.styra.opa.springboot.OPAAuthorizationManager;
import com.styra.opa.springboot.autoconfigure.OPAProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

import java.util.Map;
import java.util.function.Supplier;

/**
 * By defining a bean which implements this interface, clients could customize OPA {@code input.context}.
 */
@FunctionalInterface
public interface OPAInputContextCustomizer {

    /**
     * Customizes {@code context} {@link Map}. This method could return {@code null}.
     * @param context contains:
     * <ul>
     *     <li>{@value InputConstants#CONTEXT_TYPE}: {@value OPAProperties.Request.Context#DEFAULT_TYPE}
     *     (configurable via {@code opa.request.context.type} property)</li>
     *     <li>{@value InputConstants#CONTEXT_HOST}: {@link HttpServletRequest#getRemoteHost()}</li>
     *     <li>{@value InputConstants#CONTEXT_IP}: {@link HttpServletRequest#getRemoteAddr()}</li>
     *     <li>{@value InputConstants#CONTEXT_PORT}: {@link HttpServletRequest#getRemotePort()}</li>
     *     <li>{@value InputConstants#CONTEXT_DATA}:
     *     {@link ContextDataProvider#getContextData(Supplier, RequestAuthorizationContext)} (if a
     *     {@link ContextDataProvider} gets passed to an {@link OPAAuthorizationManager} constructor)</li>
     * </ul>
     * @return if not null, should at least contains this key:
     * <ul>
     *     <li>{@value InputConstants#CONTEXT_TYPE}</li>
     * </ul>
     */
    Map<String, Object> customize(Authentication authentication,
                                  RequestAuthorizationContext requestAuthorizationContext, Map<String, Object> context);
}
