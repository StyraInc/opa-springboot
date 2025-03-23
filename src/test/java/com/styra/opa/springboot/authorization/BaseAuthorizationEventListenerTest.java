package com.styra.opa.springboot.authorization;

import com.styra.opa.OPAClient;
import com.styra.opa.springboot.BaseIntegrationTest;
import com.styra.opa.springboot.OPAAuthorizationManager;
import com.styra.opa.springboot.OPAPathSelector;
import com.styra.opa.springboot.autoconfigure.OPAAutoConfiguration;
import com.styra.opa.springboot.autoconfigure.OPAProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.authorization.event.AuthorizationDeniedEvent;
import org.springframework.security.authorization.event.AuthorizationGrantedEvent;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@AutoConfigureMockMvc
@SpringBootTest(classes = {BaseAuthorizationEventListenerTest.CustomOPAConfig.class,
    BaseAuthorizationEventListenerTest.CustomAuthorizationEventConfig.class, OPAAutoConfiguration.class})
public class BaseAuthorizationEventListenerTest extends BaseIntegrationTest {

    @Autowired
    private CustomAuthorizationEventConfig.CustomAuthorizationEventListener authorizationEventListener;
    @Autowired
    private MockMvc mockMvc;

    public CustomAuthorizationEventConfig.CustomAuthorizationEventListener getAuthorizationEventListener() {
        return authorizationEventListener;
    }

    public MockMvc getMockMvc() {
        return mockMvc;
    }

    @Order(Ordered.HIGHEST_PRECEDENCE + 10)
    @EnableWebSecurity
    @TestConfiguration
    public static class CustomAuthorizationEventConfig {

        @Autowired
        private OPAAuthorizationManager opaAuthorizationManager;

        @Component
        public static class CustomAuthorizationEventListener {

            private AuthorizationDeniedEvent<?> lastAuthorizationDeniedEvent;
            private AuthorizationGrantedEvent<?> lastAuthorizationGrantedEvent;

            @EventListener
            public void onDeny(AuthorizationDeniedEvent<?> denied) {
                lastAuthorizationDeniedEvent = denied;
            }

            @EventListener
            public void onGrant(AuthorizationGrantedEvent<?> granted) {
                lastAuthorizationGrantedEvent = granted;
            }

            public AuthorizationDeniedEvent<?> getLastAuthorizationDeniedEvent() {
                return lastAuthorizationDeniedEvent;
            }

            public AuthorizationGrantedEvent<?> getLastAuthorizationGrantedEvent() {
                return lastAuthorizationGrantedEvent;
            }
        }

        @RestController
        @RequestMapping("/test")
        public static class TestController {

            @GetMapping("/hello")
            public String sayHello() {
                return "Hello world!";
            }
        }

        @Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            return http.authorizeHttpRequests(authorize -> authorize.anyRequest().access(opaAuthorizationManager))
                .build();
        }
    }

    @Order(Ordered.HIGHEST_PRECEDENCE)
    @EnableWebSecurity
    @TestConfiguration
    public static class CustomOPAConfig {
        @Bean
        public OPAClient opaClient(OPAProperties opaProperties) {
            return new OPAClient(opaProperties.getUrl(), HEADERS);
        }

        @Bean
        public OPAPathSelector opaPathSelector() {
            return (authentication, requestAuthorizationContext, opaInput) -> {
                if (((User) authentication.getPrincipal()).getUsername().equals("granted_user")) {
                    return "policy/echo";
                } else {
                    return "policy/decision_always_false";
                }
            };
        }
    }
}
