package com.styra.opa.springboot.input;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.access.AccessDeniedException;

import java.util.HashMap;

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
import static com.styra.opa.springboot.input.OPAInputValidator.EXCEPTION_MESSAGE_TEMPLATE;
import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class OPAInputValidatorTest {

    @Nested
    @Import(OPAInputSubjectTypeValidationTest.OPAInputSubjectCustomizerConfig.class)
    class OPAInputSubjectTypeValidationTest extends BaseOpaInputCustomizerIntegrationTest {
        @Test
        public void testSubjectTypeValidation() {
            var exception = assertThrows(AccessDeniedException.class, this::callAuthorizationManagerAndVerify);
            assertEquals(format(EXCEPTION_MESSAGE_TEMPLATE, SUBJECT, SUBJECT_TYPE), exception.getMessage());
        }

        @Order(Ordered.HIGHEST_PRECEDENCE)
        @TestConfiguration
        public static class OPAInputSubjectCustomizerConfig {
            @Bean
            public OPAInputSubjectCustomizer opaInputSubjectCustomizer() {
                return (authentication, requestAuthorizationContext, subject) -> {
                    var customSubject = new HashMap<>(subject);
                    customSubject.remove(SUBJECT_TYPE);
                    return customSubject;
                };
            }
        }
    }

    @Nested
    @Import(OPAInputSubjectIdValidationTest.OPAInputSubjectCustomizerConfig.class)
    class OPAInputSubjectIdValidationTest extends BaseOpaInputCustomizerIntegrationTest {
        @Test
        public void testSubjectIdValidation() {
            var exception = assertThrows(AccessDeniedException.class, this::callAuthorizationManagerAndVerify);
            assertEquals(format(EXCEPTION_MESSAGE_TEMPLATE, SUBJECT, SUBJECT_ID), exception.getMessage());
        }

        @Order(Ordered.HIGHEST_PRECEDENCE)
        @TestConfiguration
        public static class OPAInputSubjectCustomizerConfig {
            @Bean
            public OPAInputSubjectCustomizer opaInputSubjectCustomizer() {
                return (authentication, requestAuthorizationContext, subject) -> {
                    var customSubject = new HashMap<>(subject);
                    customSubject.remove(SUBJECT_ID);
                    return customSubject;
                };
            }
        }
    }

    @Nested
    @Import(OPAInputResourceTypeValidationTest.OPAInputResourceCustomizerConfig.class)
    class OPAInputResourceTypeValidationTest extends BaseOpaInputCustomizerIntegrationTest {
        @Test
        public void testResourceTypeValidation() {
            var exception = assertThrows(AccessDeniedException.class, this::callAuthorizationManagerAndVerify);
            assertEquals(format(EXCEPTION_MESSAGE_TEMPLATE, RESOURCE, RESOURCE_TYPE), exception.getMessage());
        }

        @Order(Ordered.HIGHEST_PRECEDENCE)
        @TestConfiguration
        public static class OPAInputResourceCustomizerConfig {
            @Bean
            public OPAInputResourceCustomizer opaInputResourceCustomizer() {
                return (authentication, requestAuthorizationContext, resource) -> {
                    var customResource = new HashMap<>(resource);
                    customResource.remove(RESOURCE_TYPE);
                    return customResource;
                };
            }
        }
    }

    @Nested
    @Import(OPAInputResourceIdValidationTest.OPAInputResourceCustomizerConfig.class)
    class OPAInputResourceIdValidationTest extends BaseOpaInputCustomizerIntegrationTest {
        @Test
        public void testResourceTypeValidation() {
            var exception = assertThrows(AccessDeniedException.class, this::callAuthorizationManagerAndVerify);
            assertEquals(format(EXCEPTION_MESSAGE_TEMPLATE, RESOURCE, RESOURCE_ID), exception.getMessage());
        }

        @Order(Ordered.HIGHEST_PRECEDENCE)
        @TestConfiguration
        public static class OPAInputResourceCustomizerConfig {
            @Bean
            public OPAInputResourceCustomizer opaInputResourceCustomizer() {
                return (authentication, requestAuthorizationContext, resource) -> {
                    var customResource = new HashMap<>(resource);
                    customResource.remove(RESOURCE_ID);
                    return customResource;
                };
            }
        }
    }

    @Nested
    @Import(OPAInputActionNameValidationTest.OPAInputActionCustomizerConfig.class)
    class OPAInputActionNameValidationTest extends BaseOpaInputCustomizerIntegrationTest {
        @Test
        public void testActionNameValidation() {
            var exception = assertThrows(AccessDeniedException.class, this::callAuthorizationManagerAndVerify);
            assertEquals(format(EXCEPTION_MESSAGE_TEMPLATE, ACTION, ACTION_NAME), exception.getMessage());
        }

        @Order(Ordered.HIGHEST_PRECEDENCE)
        @TestConfiguration
        public static class OPAInputActionCustomizerConfig {
            @Bean
            public OPAInputActionCustomizer opaInputActionCustomizer() {
                return (authentication, requestAuthorizationContext, action) -> {
                    var customAction = new HashMap<>(action);
                    customAction.remove(ACTION_NAME);
                    return customAction;
                };
            }
        }
    }

    @Nested
    @Import(OPAInputContextTypeValidationTest.OPAInputContextCustomizerConfig.class)
    class OPAInputContextTypeValidationTest extends BaseOpaInputCustomizerIntegrationTest {
        @Test
        public void testContextTypeValidation() {
            var exception = assertThrows(AccessDeniedException.class, this::callAuthorizationManagerAndVerify);
            assertEquals(format(EXCEPTION_MESSAGE_TEMPLATE, CONTEXT, CONTEXT_TYPE), exception.getMessage());
        }

        @Order(Ordered.HIGHEST_PRECEDENCE)
        @TestConfiguration
        public static class OPAInputContextCustomizerConfig {
            @Bean
            public OPAInputContextCustomizer opaInputContextCustomizer() {
                return (authentication, requestAuthorizationContext, context) -> {
                    var customContext = new HashMap<>(context);
                    customContext.remove(CONTEXT_TYPE);
                    return customContext;
                };
            }
        }
    }
}
