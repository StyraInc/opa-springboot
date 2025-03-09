package com.styra.opa.springboot.autoconfigure;

import com.styra.opa.OPAClient;
import com.styra.opa.springboot.OPAAuthorizationManager;
import com.styra.opa.springboot.OPAPathSelector;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(classes = OPAAutoConfiguration.class)
public class OPAAutoConfigurationTest {

    @TestPropertySource(properties = { "opa.response.context.reason-key=fr" })
    @Nested
    public class DefaultOPAAutoConfigurationTest {

        @Autowired(required = false)
        private OPAProperties opaProperties;
        @Autowired(required = false)
        private OPAClient opaClient;
        @Autowired(required = false)
        private OPAPathSelector opaPathSelector;
        @Autowired(required = false)
        private OPAAuthorizationManager opaAuthorizationManager;

        @Test
        public void testDefaultBeansExistence() {
            assertNotNull(opaProperties);
            assertNotNull(opaClient);
            assertNotNull(opaPathSelector);
            assertNotNull(opaAuthorizationManager);
        }

        /**
         * Make sure that {@link #opaProperties} bean is autowired in {@link #opaAuthorizationManager}.
         */
        @Test
        public void testOPAPropertiesBeanAutowiring() {
            assertEquals("fr", opaProperties.getResponse().getContext().getReasonKey());
            assertEquals("fr", opaAuthorizationManager.getReasonKey());
        }
    }

    @Import(OPAAutoConfigurationTestWithCustomOPAClient.CustomOPAClientConfiguration.class)
    @Nested
    public class OPAAutoConfigurationTestWithCustomOPAClient {

        @Autowired(required = false)
        private Map<String, OPAClient> opaClients;

        @Test
        public void testCustomOPAClientBeanExistence() {
            assertNotNull(opaClients);
            assertEquals(1, opaClients.size());
            assertNotNull(opaClients.get("customOPAClient"));
        }

        @Configuration
        public static class CustomOPAClientConfiguration {

            @Bean
            public OPAClient customOPAClient() {
                return new OPAClient("http://localhost:8182");
            }
        }
    }

    @Import(OPAAutoConfigurationTestWithCustomOPAPathSelector.CustomOPAPathSelectorConfiguration.class)
    @Nested
    public class OPAAutoConfigurationTestWithCustomOPAPathSelector {

        @Autowired(required = false)
        private Map<String, OPAPathSelector> opaPathSelectors;

        @Test
        public void testCustomOPAPathSelectorBeanExistence() {
            assertNotNull(opaPathSelectors);
            assertEquals(1, opaPathSelectors.size());
            assertNotNull(opaPathSelectors.get("customOPAPathSelector"));
        }

        @Configuration
        public static class CustomOPAPathSelectorConfiguration {

            @Bean
            public OPAPathSelector customOPAPathSelector() {
                return (authentication, requestAuthorizationContext, opaInput) -> "foo/bar";
            }
        }
    }

    @Import(OPAAutoConfigurationTestWithCustomOPAAuthorizationManager.CustomOPAAuthorizationManagerConfiguration.class)
    @Nested
    public class OPAAutoConfigurationTestWithCustomOPAAuthorizationManager {

        @Autowired(required = false)
        private Map<String, OPAAuthorizationManager> opaAuthorizationManagers;

        @Test
        public void testCustomOPAAuthorizationMangerBeanExistence() {
            assertNotNull(opaAuthorizationManagers);
            assertEquals(1, opaAuthorizationManagers.size());
            assertNotNull(opaAuthorizationManagers.get("customOPAAuthorizationManager"));
        }

        @Configuration
        public static class CustomOPAAuthorizationManagerConfiguration {

            @Bean
            public OPAAuthorizationManager customOPAAuthorizationManager() {
                return new OPAAuthorizationManager("foo/bar2");
            }
        }
    }
}
