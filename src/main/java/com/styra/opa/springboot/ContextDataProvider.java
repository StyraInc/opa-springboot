package com.styra.opa.springboot;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

import java.util.function.Supplier;

/**
 * This interface can be used to expose additional information to the OPA policy. Data returned by
 * {@link #getContextData(Supplier, RequestAuthorizationContext)} is placed in {@code input.context.data}. The
 * returned object must be JSON serializable.
 */
@FunctionalInterface
public interface ContextDataProvider {
    Object getContextData(Supplier<Authentication> authentication, RequestAuthorizationContext object);
}
