package com.styra.opa.springboot;

import com.styra.opa.OPAClient;
import com.styra.opa.springboot.autoconfigure.OPAProperties;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.access.AccessDeniedException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@Nested
@Import(OPAPathSelectorTest.CustomOPAConfig.class)
public class OPAPathSelectorTest extends BaseIntegrationTest {

    @Autowired
    private OPAAuthorizationManager opaAuthorizationManager;

    @Test
    public void testOPAPathSelectorAlwaysTruePolicy() {
        var mockAuth = createMockAuthentication();
        when(mockAuth.getPrincipal()).thenReturn("testuser_allowed");
        when(authenticationSupplier.get()).thenReturn(mockAuth);
        assertDoesNotThrow(() -> opaAuthorizationManager.verify(authenticationSupplier, context));
    }

    @Test
    public void testOPAPathSelectorAlwaysFalsePolicy() {
        var mockAuth = createMockAuthentication();
        when(mockAuth.getPrincipal()).thenReturn("testuser_denied");
        when(authenticationSupplier.get()).thenReturn(mockAuth);
        assertThrows(AccessDeniedException.class,
            () -> opaAuthorizationManager.verify(authenticationSupplier, context));
    }

    @Order(Ordered.HIGHEST_PRECEDENCE)
    @TestConfiguration
    public static class CustomOPAConfig {
        @Bean
        public OPAClient opaClient(OPAProperties opaProperties) {
            return new OPAClient(opaProperties.getUrl(), HEADERS);
        }

        @Bean
        public OPAPathSelector opaPathSelector() {
            return (authentication, requestAuthorizationContext, opaInput) -> {
                if (authentication.getPrincipal().equals("testuser_allowed")) {
                    return "policy/decision_always_true";
                } else {
                    return "policy/decision_always_false";
                }
            };
        }
    }
}
