package com.styra.opa.springboot;

import java.util.function.Supplier;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

/**
 * This interface can be used to expose additional information to the OPA
 * policy via the context field. Data returned by getContextData() is placed
 * in input.context.data. The returned object must be JSON serializeable.
 */
@FunctionalInterface
interface ContextDataProvider {
    Object getContextData(
        Supplier<Authentication> authentication,
        RequestAuthorizationContext object
    );
}
