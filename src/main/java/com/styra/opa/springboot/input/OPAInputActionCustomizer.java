package com.styra.opa.springboot.input;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

import java.util.Map;

/**
 * By defining a bean which implements this interface, clients could customize OPA {@code input.action}.
 */
@FunctionalInterface
public interface OPAInputActionCustomizer {

    /**
     * Customizes {@code action} {@link Map}.
     * @param action contains:
     * <ul>
     *     <li>{@value InputConstants#ACTION_NAME}: {@link HttpServletRequest#getMethod()}</li>
     *     <li>{@value InputConstants#ACTION_PROTOCOL}: {@link HttpServletRequest#getProtocol()}</li>
     *     <li>{@value InputConstants#ACTION_HEADERS}: {@link HttpServletRequest} headers</li>
     * </ul>
     * @return should at least contains this key:
     * <ul>
     *     <li>{@value InputConstants#ACTION_NAME}</li>
     * </ul>
     */
    Map<String, Object> customize(Authentication authentication,
                                  RequestAuthorizationContext requestAuthorizationContext, Map<String, Object> action);
}
