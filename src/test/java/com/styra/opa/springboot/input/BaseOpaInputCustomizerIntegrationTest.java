package com.styra.opa.springboot.input;

import com.styra.opa.OPAClient;
import com.styra.opa.springboot.BaseIntegrationTest;
import com.styra.opa.springboot.OPAAuthorizationManager;
import com.styra.opa.springboot.OPAPathSelector;
import com.styra.opa.springboot.autoconfigure.OPAProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@Import(BaseOpaInputCustomizerIntegrationTest.CustomOPAInputCustomizerConfig.class)
public class BaseOpaInputCustomizerIntegrationTest extends BaseIntegrationTest {
    @Autowired
    private OPAAuthorizationManager opaAuthorizationManager;

    protected Map<String, Object> callAuthorizationManagerAndVerify() {
        var expectedResponseContextData = createNullMockAuthOPAInput();
        var mockAuth = createNullMockAuthentication();
        when(authenticationSupplier.get()).thenReturn(mockAuth);

        var actualResponse = getOpaAuthorizationManager().opaRequest(authenticationSupplier, context);

        assertNotNull(actualResponse.getContext());
        assertNotNull(actualResponse.getContext().getData());
        var actualResponseContextData = actualResponse.getContext().getData();
        assertNotEquals(expectedResponseContextData, actualResponseContextData);
        return actualResponseContextData;
    }

    protected OPAAuthorizationManager getOpaAuthorizationManager() {
        return opaAuthorizationManager;
    }

    @Order(Ordered.HIGHEST_PRECEDENCE)
    @TestConfiguration
    public static class CustomOPAInputCustomizerConfig {
        @Bean
        public OPAClient opaClient(OPAProperties opaProperties) {
            return new OPAClient(opaProperties.getUrl(), HEADERS);
        }

        @Bean
        public OPAPathSelector opaPathSelector() {
            return (authentication, requestAuthorizationContext, opaInput) -> "policy/echo";
        }
    }
}
