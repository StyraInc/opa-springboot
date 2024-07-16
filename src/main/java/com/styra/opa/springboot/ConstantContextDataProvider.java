package com.styra.opa.springboot;

import java.util.function.Supplier;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

/**
 * This helper class allows creating a ContextDataProvider which always returns
 * the same constant value. This is useful for tests, and also for situations
 * where the extra data to inject does not change during runtime.
 */
public class ConstantContextDataProvider implements ContextDataProvider {

    private Object data;

    public ConstantContextDataProvider(Object newData) {
        this.data = newData;
    }

    public Object getContextData(
        Supplier<Authentication> authentication,
        RequestAuthorizationContext object
    ) {
        return this.data;
    }
}
