package com.styra.opa.springboot.input;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import java.util.HashMap;
import java.util.Map;

import static com.styra.opa.springboot.input.InputConstants.CONTEXT;
import static com.styra.opa.springboot.input.InputConstants.CONTEXT_TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class OPAInputContextCustomizerTest {

    @Nested
    @Import(NullOPAInputContextCustomizerTest.NullOPAInputContextCustomizerConfig.class)
    class NullOPAInputContextCustomizerTest extends BaseOpaInputCustomizerIntegrationTest {
        @Test
        public void testNullOPAInputContextCustomizer() {
            var actualResponseContextData = callAuthorizationManagerAndVerify();
            assertNull(actualResponseContextData.get(CONTEXT));
        }

        @Order(Ordered.HIGHEST_PRECEDENCE)
        @TestConfiguration
        public static class NullOPAInputContextCustomizerConfig {
            @Bean
            public OPAInputContextCustomizer opaInputContextCustomizer() {
                return (authentication, requestAuthorizationContext, context) -> null;
            }
        }
    }

    @Nested
    @Import(NotNullOPAInputContextCustomizerTest.NotNullOPAInputContextCustomizerConfig.class)
    class NotNullOPAInputContextCustomizerTest extends BaseOpaInputCustomizerIntegrationTest {

        @Test
        @SuppressWarnings("unchecked")
        public void testNotNullOPAInputContextCustomizer() {
            var actualResponseContextData = callAuthorizationManagerAndVerify();
            assertNotNull(actualResponseContextData.get(CONTEXT));
            var actualContext = (Map<String, Object>) actualResponseContextData.get(CONTEXT);
            assertEquals("websocket", actualContext.get(CONTEXT_TYPE));
            assertEquals("context_value", actualContext.get("context_key"));
        }

        @Order(Ordered.HIGHEST_PRECEDENCE)
        @TestConfiguration
        public static class NotNullOPAInputContextCustomizerConfig {
            @Bean
            public OPAInputContextCustomizer opaInputContextCustomizer() {
                return (authentication, requestAuthorizationContext, context) -> {
                    var customContext = new HashMap<>(context);
                    customContext.put(CONTEXT_TYPE, "websocket");
                    customContext.put("context_key", "context_value");
                    return customContext;
                };
            }
        }
    }
}
