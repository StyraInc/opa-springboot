package com.styra.opa.springboot.input;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import java.util.HashMap;
import java.util.Map;

import static com.styra.opa.springboot.input.InputConstants.ACTION;
import static com.styra.opa.springboot.input.InputConstants.ACTION_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Import(OPAInputActionCustomizerTest.OPAInputActionCustomizerConfig.class)
public class OPAInputActionCustomizerTest extends BaseOpaInputCustomizerIntegrationTest {

    @Test
    public void testOPAInputActionCustomizer() {
        var actualResponseContextData = callAuthorizationManagerAndVerify();
        assertNotNull(actualResponseContextData.get(ACTION));
        var actualAction = (Map<String, Object>) actualResponseContextData.get(ACTION);
        assertEquals("read", actualAction.get(ACTION_NAME));
        assertEquals("action_value", actualAction.get("action_key"));
    }

    @Order(Ordered.HIGHEST_PRECEDENCE)
    @TestConfiguration
    public static class OPAInputActionCustomizerConfig {
        @Bean
        public OPAInputActionCustomizer opaInputActionCustomizer() {
            return (authentication, requestAuthorizationContext, action) -> {
                var customAction = new HashMap<>(action);
                customAction.put(ACTION_NAME, "read");
                customAction.put("action_key", "action_value");
                return customAction;
            };
        }
    }
}
