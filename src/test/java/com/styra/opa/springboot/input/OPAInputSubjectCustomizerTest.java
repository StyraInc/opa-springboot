package com.styra.opa.springboot.input;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import java.util.HashMap;
import java.util.Map;

import static com.styra.opa.springboot.input.InputConstants.SUBJECT;
import static com.styra.opa.springboot.input.InputConstants.SUBJECT_TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Import(OPAInputSubjectCustomizerTest.OPAInputSubjectCustomizerConfig.class)
public class OPAInputSubjectCustomizerTest extends BaseOpaInputCustomizerIntegrationTest {

    @Test
    @SuppressWarnings("unchecked")
    public void testOPAInputSubjectCustomizer() {
        Map<String, Object> actualResponseContextData = callAuthorizationManagerAndVerify();
        assertNotNull(actualResponseContextData.get(SUBJECT));
        Map<String, Object> actualSubject = (Map<String, Object>) actualResponseContextData.get(SUBJECT);
        assertEquals("oauth2_resource_owner", actualSubject.get(SUBJECT_TYPE));
        assertEquals("subject_value", actualSubject.get("subject_key"));
    }

    @Order(Ordered.HIGHEST_PRECEDENCE)
    @TestConfiguration
    public static class OPAInputSubjectCustomizerConfig {
        @Bean
        public OPAInputSubjectCustomizer opaInputSubjectCustomizer() {
            return (authentication, requestAuthorizationContext, subject) -> {
                var customSubject = new HashMap<>(subject);
                customSubject.put(SUBJECT_TYPE, "oauth2_resource_owner");
                customSubject.put("subject_key", "subject_value");
                return customSubject;
            };
        }
    }
}
