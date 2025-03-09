package com.styra.opa.springboot;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

import java.util.function.Supplier;

/**
 * This helper class implements {@link ContextDataProvider} and always returns the same constant value passed in the
 * constructor. This is useful for tests, and also for situations where the extra data to inject does not change during
 * runtime.
 */
public class ConstantContextDataProvider implements ContextDataProvider {

    private final Object data;

    public ConstantContextDataProvider(Object data) {
        this.data = data;
    }

    @Override
    public Object getContextData(Supplier<Authentication> authentication, RequestAuthorizationContext object) {
        return this.data;
    }
}
