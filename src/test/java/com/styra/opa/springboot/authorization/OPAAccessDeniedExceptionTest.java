package com.styra.opa.springboot.authorization;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.styra.opa.OPAClient;
import com.styra.opa.springboot.BaseIntegrationTest;
import com.styra.opa.springboot.OPAAuthorizationManager;
import com.styra.opa.springboot.autoconfigure.OPAAutoConfiguration;
import com.styra.opa.springboot.autoconfigure.OPAProperties;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;
import org.springframework.stereotype.Component;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static com.styra.opa.springboot.input.InputConstants.SUBJECT;
import static com.styra.opa.springboot.input.InputConstants.SUBJECT_ID;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestPropertySource(properties = {"opa.path=policy/decision_always_false"})
@AutoConfigureMockMvc
@SpringBootTest(classes = {OPAAccessDeniedExceptionTest.CustomOPAConfig.class,
    OPAAccessDeniedExceptionTest.CustomAuthorizationEventConfig.class, OPAAutoConfiguration.class})
public class OPAAccessDeniedExceptionTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @WithMockUser(username = "denied_user")
    @Test
    public void testDefaultAuthorizationDeniedEvent() throws Exception {
        mockMvc.perform(get("/test/hello"))
            .andExpect(status().isForbidden())
            .andDo(print())
            .andExpect(result -> jsonPath("$.title").value("Access Denied"))
            .andExpect(result -> jsonPath("$.status").value(HttpStatus.FORBIDDEN.value()))
            .andExpect(result -> jsonPath("$.detail").value("Access denied for subject: denied_user"))
            .andExpect(result -> jsonPath("$.subject.subject_id").value("denied_user"));
    }

    @Order(Ordered.HIGHEST_PRECEDENCE + 10)
    @EnableWebSecurity
    @TestConfiguration
    public static class CustomAuthorizationEventConfig {

        @Autowired
        private OPAAuthorizationManager opaAuthorizationManager;

        @Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            return http.authorizeHttpRequests(authorize -> authorize.anyRequest().access(opaAuthorizationManager))
                .build();
        }

        @RestController
        @RequestMapping("/test")
        public static class TestController {

            @GetMapping("/hello")
            public String sayHello() {
                return "Hello world!";
            }
        }

        /**
         * Generates JSON error response based on RFC 9457.
         * @see <a href="https://datatracker.ietf.org/doc/html/rfc9457">RFC 9457 - Problem Details for HTTP APIs</a>
         */
        @Component
        public class ClientOPAAccessDeniedHandler extends AccessDeniedHandlerImpl {

            @Autowired
            private ObjectMapper objectMapper;

            @Override
            public void handle(HttpServletRequest request, HttpServletResponse response,
                               AccessDeniedException accessDeniedException) throws IOException, ServletException {
                if (!(accessDeniedException instanceof OPAAccessDeniedException opaAccessDeniedException)) {
                    super.handle(request, response, accessDeniedException);
                    return;
                }
                Map<String, Object> body = new HashMap<>();
                body.put("status", HttpStatus.FORBIDDEN.value());
                body.put("title", opaAccessDeniedException.getMessage());
                var subject =
                    (Map<String, Object>) opaAccessDeniedException.getOpaResponse().getContext().getData().get(SUBJECT);
                var subjectId = subject.get(SUBJECT_ID);
                body.put("detail", "Access denied for subject: " + subjectId);
                body.put("subject", subject);
                response.setStatus(HttpStatus.FORBIDDEN.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setCharacterEncoding(StandardCharsets.UTF_8.toString());
                response.getWriter().write(objectMapper.writeValueAsString(body));
                response.getWriter().flush();
            }
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
        public ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }
}
