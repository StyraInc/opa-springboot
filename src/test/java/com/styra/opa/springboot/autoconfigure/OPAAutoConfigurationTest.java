package com.styra.opa.springboot.autoconfigure;

import com.styra.opa.OPAClient;
import com.styra.opa.springboot.OPAAuthorizationManager;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(classes = OPAAutoConfiguration.class)
public class OPAAutoConfigurationTest {

    @Nested
    public class DefaultOPAAutoConfigurationTest {

        @Autowired(required = false)
        private OPAProperties opaProperties;
        @Autowired(required = false)
        private OPAClient opaClient;
        @Autowired(required = false)
        private OPAAuthorizationManager opaAuthorizationManager;

        @Test
        public void test() {
            assertNotNull(opaProperties);
            assertNotNull(opaClient);
            assertNotNull(opaAuthorizationManager);
        }
    }

    @Import(OPAAutoConfigurationTestWithCustomOPAClient.CustomOPAClientConfiguration.class)
    @Nested
    public class OPAAutoConfigurationTestWithCustomOPAClient {

        @Autowired(required = false)
        private Map<String, OPAClient> opaClients;

        @Test
        public void test() {
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

    @Import(OPAAutoConfigurationTestWithCustomOPAAuthorizationManager.CustomOPAAuthorizationManagerConfiguration.class)
    @Nested
    public class OPAAutoConfigurationTestWithCustomOPAAuthorizationManager {

        @Autowired(required = false)
        private Map<String, OPAAuthorizationManager> opaAuthorizationManagers;

        @Test
        public void test() {
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
