package com.styra.opa.springboot.input;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

import java.util.Map;

import static com.styra.opa.springboot.input.InputConstants.ACTION;
import static com.styra.opa.springboot.input.InputConstants.ACTION_NAME;
import static com.styra.opa.springboot.input.InputConstants.CONTEXT;
import static com.styra.opa.springboot.input.InputConstants.CONTEXT_TYPE;
import static com.styra.opa.springboot.input.InputConstants.RESOURCE;
import static com.styra.opa.springboot.input.InputConstants.RESOURCE_ID;
import static com.styra.opa.springboot.input.InputConstants.RESOURCE_TYPE;
import static com.styra.opa.springboot.input.InputConstants.SUBJECT;
import static com.styra.opa.springboot.input.InputConstants.SUBJECT_ID;
import static com.styra.opa.springboot.input.InputConstants.SUBJECT_TYPE;
import static java.lang.String.format;

/**
 * Makes sure that mandatory OPA {@code input} properties are available.
 */
public final class OPAInputValidator {

    public static final String EXCEPTION_MESSAGE_TEMPLATE = "OPA input must contain '%s.%s'";

    /**
     * Validates {@code input} {@link Map}.
     * @param input which will be passed to the OPA server as request body.
     * @throws AccessDeniedException if {code input} does not contain any of these keys:
     * <ul>
     *     <li>{@value InputConstants#SUBJECT}.{@value InputConstants#SUBJECT_TYPE}</li>
     *     <li>{@value InputConstants#SUBJECT}.{@value InputConstants#SUBJECT_ID}</li>
     *     <li>{@value InputConstants#RESOURCE}.{@value InputConstants#RESOURCE_TYPE}</li>
     *     <li>{@value InputConstants#RESOURCE}.{@value InputConstants#RESOURCE_ID}</li>
     *     <li>{@value InputConstants#ACTION}.{@value InputConstants#ACTION_NAME}</li>
     *     <li>If {@value InputConstants#CONTEXT} is available, {@value InputConstants#CONTEXT}.
     *     {@value InputConstants#CONTEXT_TYPE}</li>
     * </ul>
     */
    public void validate(Authentication authentication, RequestAuthorizationContext requestAuthorizationContext,
                         Map<String, Object> input) throws AccessDeniedException {
        validateKey(input, SUBJECT, SUBJECT_TYPE);
        validateKey(input, SUBJECT, SUBJECT_ID);
        validateKey(input, RESOURCE, RESOURCE_TYPE);
        validateKey(input, RESOURCE, RESOURCE_ID);
        validateKey(input, ACTION, ACTION_NAME);
        if (input.get(CONTEXT) != null) {
            validateKey(input, CONTEXT, CONTEXT_TYPE);
        }
    }

    @SuppressWarnings("unchecked")
    private void validateKey(Map<String, Object> input, String entity, String key) {
        Map<String, Object> entityMap = (Map<String, Object>) input.get(entity);
        if (entityMap.get(key) == null) {
            throw new AccessDeniedException(format(EXCEPTION_MESSAGE_TEMPLATE, entity, key));
        }
    }
}
