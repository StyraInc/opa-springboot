package com.styra.opa.springboot.input;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import java.util.HashMap;
import java.util.Map;

import static com.styra.opa.springboot.input.InputConstants.RESOURCE;
import static com.styra.opa.springboot.input.InputConstants.RESOURCE_TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Import(OPAInputResourceCustomizerTest.OPAInputResourceCustomizerConfig.class)
public class OPAInputResourceCustomizerTest extends BaseOpaInputCustomizerIntegrationTest {

    @Test
    @SuppressWarnings("unchecked")
    public void testOPAInputResourceCustomizer() {
        var actualResponseContextData = callAuthorizationManagerAndVerify();
        assertNotNull(actualResponseContextData.get(RESOURCE));
        var actualResource = (Map<String, Object>) actualResponseContextData.get(RESOURCE);
        assertEquals("stomp_endpoint", actualResource.get(RESOURCE_TYPE));
        assertEquals("resource_value", actualResource.get("resource_key"));
    }

    @Order(Ordered.HIGHEST_PRECEDENCE)
    @TestConfiguration
    public static class OPAInputResourceCustomizerConfig {
        @Bean
        public OPAInputResourceCustomizer opaInputResourceCustomizer() {
            return (authentication, requestAuthorizationContext, resource) -> {
                var customResource = new HashMap<>(resource);
                customResource.put(RESOURCE_TYPE, "stomp_endpoint");
                customResource.put("resource_key", "resource_value");
                return customResource;
            };
        }
    }
}
