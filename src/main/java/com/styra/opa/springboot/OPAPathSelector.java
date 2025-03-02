package com.styra.opa.springboot;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

import java.util.Map;

/**
 * Select target OPA path based on {@link Authentication}, {@link RequestAuthorizationContext}, and
 * opaRequestBody {@link Map}.
 */
@FunctionalInterface
public interface OPAPathSelector {
    String selectPath(Authentication authentication, RequestAuthorizationContext requestAuthorizationContext,
                      Map<String, Object> opaRequestBody);
}
