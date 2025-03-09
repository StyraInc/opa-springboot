package com.styra.opa.springboot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.styra.opa.OPAClient;
import com.styra.opa.springboot.autoconfigure.OPAAutoConfiguration;
import com.styra.opa.springboot.autoconfigure.OPAProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

import static com.styra.opa.springboot.OPAAuthorizationManager.ACTION;
import static com.styra.opa.springboot.OPAAuthorizationManager.ACTION_HEADERS;
import static com.styra.opa.springboot.OPAAuthorizationManager.ACTION_NAME;
import static com.styra.opa.springboot.OPAAuthorizationManager.ACTION_PROTOCOL;
import static com.styra.opa.springboot.OPAAuthorizationManager.CONTEXT;
import static com.styra.opa.springboot.OPAAuthorizationManager.CONTEXT_DATA;
import static com.styra.opa.springboot.OPAAuthorizationManager.CONTEXT_HOST;
import static com.styra.opa.springboot.OPAAuthorizationManager.CONTEXT_IP;
import static com.styra.opa.springboot.OPAAuthorizationManager.CONTEXT_PORT;
import static com.styra.opa.springboot.OPAAuthorizationManager.CONTEXT_TYPE;
import static com.styra.opa.springboot.OPAAuthorizationManager.RESOURCE;
import static com.styra.opa.springboot.OPAAuthorizationManager.RESOURCE_ID;
import static com.styra.opa.springboot.OPAAuthorizationManager.RESOURCE_TYPE;
import static com.styra.opa.springboot.OPAAuthorizationManager.SUBJECT;
import static com.styra.opa.springboot.OPAAuthorizationManager.SUBJECT_AUTHORITIES;
import static com.styra.opa.springboot.OPAAuthorizationManager.SUBJECT_DETAILS;
import static com.styra.opa.springboot.OPAAuthorizationManager.SUBJECT_ID;
import static com.styra.opa.springboot.OPAAuthorizationManager.SUBJECT_TYPE;
import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = OPAAutoConfiguration.class)
@Testcontainers
class OPAAuthorizationManagerTest {

    private static final int OPA_PORT = 8181;
    /**
     * Besides OPA default port ({@code opaPort}), OPA is exposed under {@code localhost:8282/customprefix} using
     * Nginx.<br/>
     * Nginx configuration is provided in {@code src/test/resources/nginx.conf} file.
     */
    private static final int ALT_PORT = 8282;
    private static final Map<String, String> HEADERS = Map.ofEntries(entry("Authorization", "Bearer supersecret"));

    //CHECKSTYLE:OFF
    /*
     * Checkstyle is disabled here because it wants opaContainer to 'be private and have accessor methods', which seems
     * pointless and will probably mess up test containers.
     */
    @Container
    public static GenericContainer<?> opaContainer = new GenericContainer<>(
        new ImageFromDockerfile()
            // .withFileFromClasspath(path_in_build_context, path_in_resources_dir)
            .withFileFromClasspath("Dockerfile", "opa.Dockerfile")
            .withFileFromClasspath("nginx.conf", "nginx.conf")
            .withFileFromClasspath("entrypoint.sh", "entrypoint.sh")
    )
        .withExposedPorts(OPA_PORT, ALT_PORT)
        .withFileSystemBind("./testdata/simple", "/policy", BindMode.READ_ONLY)
        .withCommand("run -s --authentication=token --authorization=basic --bundle /policy --addr=0.0.0.0:" + OPA_PORT)
        .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger(OPAAuthorizationManagerTest.class)));
    //CHECKSTYLE:ON

    @Mock
    private Supplier<Authentication> authenticationSupplier;

    @Mock
    private RequestAuthorizationContext context;

    @Mock
    private HttpServletRequest httpServletRequest;

    private final ObjectMapper mapper = new ObjectMapper();

    private String address;
    private String altAddress;

    @DynamicPropertySource
    static void registerOpaProperties(DynamicPropertyRegistry registry) {
        registry.add("opa.url",
            () -> String.format("http://%s:%d", opaContainer.getHost(), opaContainer.getMappedPort(OPA_PORT)));
    }

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(context.getRequest()).thenReturn(httpServletRequest);

        HashMap<String, String> mockHeaders = new HashMap<>();
        mockHeaders.put("UnitTestHeader", "123abc");
        when(httpServletRequest.getHeaderNames()).thenReturn(Collections.enumeration(mockHeaders.keySet()));
        when(httpServletRequest.getHeader(anyString())).thenAnswer(invocation -> {
            String headerName = invocation.getArgument(0);
            return mockHeaders.get(headerName);
        });

        when(httpServletRequest.getServletPath()).thenReturn("unit/test");
        when(httpServletRequest.getMethod()).thenReturn("GET");
        when(httpServletRequest.getProtocol()).thenReturn("HTTP/1.1");
        when(httpServletRequest.getRemoteHost()).thenReturn("example.com");
        when(httpServletRequest.getRemoteAddr()).thenReturn("192.0.2.123");

        address = "http://" + opaContainer.getHost() + ":" + opaContainer.getMappedPort(OPA_PORT);
        altAddress = "http://" + opaContainer.getHost() + ":" + opaContainer.getMappedPort(ALT_PORT) + "/customprefix";
    }

    @AfterEach
    public void dumpLogs() {
        System.out.println("==== container logs from OPA container ====");
        System.out.println(opaContainer.getLogs());
    }

    /**
     * This test just makes sure that we can reach the OPAClient health endpoint and that it returns the expected body.
     */
    @Test
    public void testOPAHealth() {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(address + "/health")).build();
        HttpResponse<String> response = null;

        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
            //CHECKSTYLE:OFF
        } catch (Exception e) {
            //CHECKSTYLE:ON
            System.out.println("exception: " + e);
            assertNull(e);
        }

        String responseBody = response.body();
        assertEquals("{}\n", responseBody);
    }

    /**
     * This makes sure that we can also successfully reach the OPA health API on the "alternate", reverse-proxy based
     * OPA that has a URL prefix.
     */
    @Test
    public void testOPAHealthAlternate() {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(altAddress + "/health")).build();
        HttpResponse<String> response = null;

        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
            //CHECKSTYLE:OFF
        } catch (Exception e) {
            //CHECKSTYLE:ON
            System.out.println("exception: " + e);
            assertNull(e);
        }

        String responseBody = response.body();
        assertEquals("{}\n", responseBody);
    }

    /**
     * Make sure that with a simple always-allow rule, we allow all requests, using
     * {@link OPAAuthorizationManager#check(Supplier, RequestAuthorizationContext)}.
     */
    @Test
    public void testOPAAuthorizationManagerSimpleAllow() {
        Authentication mockAuth = this.createMockAuthentication();
        when(authenticationSupplier.get()).thenReturn(mockAuth);
        OPAClient opaClient = new OPAClient(address, HEADERS);
        OPAAuthorizationManager opaAuthorizationManager =
            new OPAAuthorizationManager(opaClient, "policy/decision_always_true");
        AuthorizationDecision actual = opaAuthorizationManager.check(this.authenticationSupplier, this.context);
        assertTrue(actual.isGranted());
    }

    /**
     * Make sure that with a simple always-deny rule, we deny all requests, using
     * {@link OPAAuthorizationManager#check(Supplier, RequestAuthorizationContext)}.
     */
    @Test
    public void testOPAAuthorizationManagerSimpleDeny() {
        Authentication mockAuth = this.createMockAuthentication();
        when(authenticationSupplier.get()).thenReturn(mockAuth);
        OPAClient opaClient = new OPAClient(address, HEADERS);
        OPAAuthorizationManager opaAuthorizationManager =
            new OPAAuthorizationManager(opaClient, "policy/decision_always_false");
        AuthorizationDecision actual = opaAuthorizationManager.check(this.authenticationSupplier, this.context);
        assertFalse(actual.isGranted());
    }

    /**
     * Make sure that with a simple always-allow rule, we allow all requests, using
     * {@link OPAAuthorizationManager#verify(Supplier, RequestAuthorizationContext)}.
     */
    @Test
    public void testOPAAuthorizationManagerSimpleAllowVerify() {
        Authentication mockAuth = this.createMockAuthentication();
        when(authenticationSupplier.get()).thenReturn(mockAuth);
        OPAClient opaClient = new OPAClient(address, HEADERS);
        OPAAuthorizationManager opaAuthorizationManager =
            new OPAAuthorizationManager(opaClient, "policy/decision_always_true");
        assertDoesNotThrow(() -> opaAuthorizationManager.verify(authenticationSupplier, context));
    }

    /**
     * Make sure that with a simple always-deny rule, we deny all requests, using
     * {@link OPAAuthorizationManager#verify(Supplier, RequestAuthorizationContext)}.
     */
    @Test
    public void testOPAAuthorizationManagerSimpleDenyVerify() {
        Authentication mockAuth = this.createMockAuthentication();
        when(authenticationSupplier.get()).thenReturn(mockAuth);
        OPAClient opaClient = new OPAClient(address, HEADERS);
        OPAAuthorizationManager opaAuthorizationManager =
            new OPAAuthorizationManager(opaClient, "policy/decision_always_false");
        assertThrows(AccessDeniedException.class,
            () -> opaAuthorizationManager.verify(authenticationSupplier, context));
    }

    /**
     * By reading back the input, we can make sure the OPA input has the right structure and content.
     */
    @Test
    public void testOPAAuthorizationManagerEcho() {
        Map<String, Object> expectedResponseContextData = Map.ofEntries(
            entry(ACTION, Map.ofEntries(
                entry(ACTION_HEADERS, Map.ofEntries(
                    entry("UnitTestHeader", "123abc")
                )),
                entry(ACTION_NAME, "GET"),
                entry(ACTION_PROTOCOL, "HTTP/1.1")
            )),
            entry(CONTEXT, Map.ofEntries(
                entry(CONTEXT_HOST, "example.com"),
                entry(CONTEXT_IP, "192.0.2.123"),
                entry(CONTEXT_PORT, 0),
                entry(CONTEXT_TYPE, OPAProperties.Request.Context.DEFAULT_TYPE),
                entry(CONTEXT_DATA, Map.ofEntries(
                    entry("hello", "world")
                ))
            )),
            entry(RESOURCE, Map.ofEntries(
                entry(RESOURCE_ID, "unit/test"),
                entry(RESOURCE_TYPE, OPAProperties.Request.Resource.DEFAULT_TYPE)
            )),
            entry(SUBJECT, Map.ofEntries(
                entry(SUBJECT_AUTHORITIES, List.of(
                    Map.ofEntries(entry("authority", "ROLE_USER")),
                    Map.ofEntries(entry("authority", "ROLE_ADMIN"))
                )),
                entry(SUBJECT_DETAILS, Map.ofEntries(
                    entry("remoteAddress", "192.0.2.123"),
                    entry("sessionId", "null")
                )),
                entry(SUBJECT_ID, "testuser"),
                entry(SUBJECT_TYPE, OPAProperties.Request.Subject.DEFAULT_TYPE)
            ))
        );

        OPAResponseContext expectedResponseContext = new OPAResponseContext();
        expectedResponseContext.setReasonUser(Map.ofEntries(
            entry("en", "echo rule always allows"),
            entry("other", "other reason key")
        ));
        expectedResponseContext.setId("0");
        expectedResponseContext.setData(expectedResponseContextData);

        OPAResponse expectedResponse = new OPAResponse();
        expectedResponse.setDecision(true);
        expectedResponse.setContext(expectedResponseContext);

        ContextDataProvider contextDataProvider = new ConstantContextDataProvider(Map.ofEntries(
            entry("hello", "world")
        ));
        Authentication mockAuth = this.createMockAuthentication();
        when(authenticationSupplier.get()).thenReturn(mockAuth);
        OPAClient opaClient = new OPAClient(address, HEADERS);
        OPAAuthorizationManager opaAuthorizationManager =
            new OPAAuthorizationManager(opaClient, "policy/echo", contextDataProvider);
        OPAResponse actualResponse = opaAuthorizationManager.opaRequest(this.authenticationSupplier, this.context);

        assertEquals(expectedResponse.getDecision(), actualResponse.getDecision());
        assertEquals(expectedResponse.getContext().getId(), actualResponse.getContext().getId());
        assertEquals(expectedResponse.getContext().getReasonUser(), actualResponse.getContext().getReasonUser());

        List<String> dataDiffs = jsonDiff(expectedResponse.getContext().getData(),
            actualResponse.getContext().getData());

        System.out.printf("#### expected context data\n%s\n", jsonPretty(expectedResponseContextData));
        System.out.printf("#### actual context data\n%s\n", jsonPretty(actualResponse.getContext().getData()));

        for (String dataDiff : dataDiffs) {
            System.out.printf("diff mismatch: %s\n", dataDiff);
        }

        assertEquals(0, dataDiffs.size());
        assertEquals("echo rule always allows", actualResponse.getReasonForDecision("en"));
        assertEquals("other reason key", actualResponse.getReasonForDecision("other"));
        assertEquals("echo rule always allows", actualResponse.getReasonForDecision("nonexistant"));
    }

    /**
     * By reading back the input, we can make sure the OPA input has the right structure and content.
     */
    @Test
    public void testOPAAuthorizationManagerNullMetadata() {
        Map<String, Object> expectedResponseContextData = Map.ofEntries(
            entry(ACTION, Map.ofEntries(
                entry(ACTION_HEADERS, Map.ofEntries(
                    entry("UnitTestHeader", "123abc")
                )),
                entry(ACTION_NAME, "GET"),
                entry(ACTION_PROTOCOL, "HTTP/1.1")
            )),
            entry(CONTEXT, Map.ofEntries(
                entry(CONTEXT_HOST, "example.com"),
                entry(CONTEXT_IP, "192.0.2.123"),
                entry(CONTEXT_PORT, 0),
                entry(CONTEXT_TYPE, "http"),
                entry(CONTEXT_DATA, Map.ofEntries(
                    entry("hello", "world")
                ))
            )),
            entry(RESOURCE, Map.ofEntries(
                entry(RESOURCE_ID, "unit/test"),
                entry(RESOURCE_TYPE, "endpoint")
            )),
            entry(SUBJECT, Map.ofEntries(
                entry(SUBJECT_ID, "testuser"),
                entry(SUBJECT_TYPE, "java_authentication")
            ))
        );

        OPAResponseContext expectedResponseContext = new OPAResponseContext();
        expectedResponseContext.setReasonUser(Map.ofEntries(
            entry("en", "echo rule always allows"),
            entry("other", "other reason key")
        ));
        expectedResponseContext.setId("0");
        expectedResponseContext.setData(expectedResponseContextData);

        OPAResponse expectedResponse = new OPAResponse();
        expectedResponse.setDecision(true);
        expectedResponse.setContext(expectedResponseContext);

        ContextDataProvider contextDataProvider = new ConstantContextDataProvider(Map.ofEntries(
            entry("hello", "world")
        ));
        Authentication mockAuth = this.createNullMockAuthentication();
        when(authenticationSupplier.get()).thenReturn(mockAuth);
        OPAClient opaClient = new OPAClient(address, HEADERS);
        OPAAuthorizationManager opaAuthorizationManager =
            new OPAAuthorizationManager(opaClient, "policy/echo", contextDataProvider);
        OPAResponse actualResponse = opaAuthorizationManager.opaRequest(this.authenticationSupplier, this.context);

        assertEquals(expectedResponse.getDecision(), actualResponse.getDecision());
        assertEquals(expectedResponse.getContext().getId(), actualResponse.getContext().getId());
        assertEquals(expectedResponse.getContext().getReasonUser(), actualResponse.getContext().getReasonUser());

        List<String> dataDiffs = jsonDiff(expectedResponse.getContext().getData(),
            actualResponse.getContext().getData());

        System.out.printf("#### expected context data\n%s\n", jsonPretty(expectedResponseContextData));
        System.out.printf("#### actual context data\n%s\n", jsonPretty(actualResponse.getContext().getData()));

        for (String dataDiff : dataDiffs) {
            System.out.printf("diff mismatch: %s\n", dataDiff);
        }

        assertEquals(0, dataDiffs.size());
        assertEquals("echo rule always allows", actualResponse.getReasonForDecision("en"));
        assertEquals("other reason key", actualResponse.getReasonForDecision("other"));
        assertEquals("echo rule always allows", actualResponse.getReasonForDecision("nonexistant"));
    }

    private Authentication createMockAuthentication() {
        Authentication mockAuth = mock(Authentication.class);
        when(mockAuth.getPrincipal()).thenReturn("testuser");
        when(mockAuth.getCredentials()).thenReturn("letmein");

        WebAuthenticationDetails details = new WebAuthenticationDetails(httpServletRequest);
        when(mockAuth.getDetails()).thenReturn(details);

        GrantedAuthority authority1 = new SimpleGrantedAuthority("ROLE_USER");
        GrantedAuthority authority2 = new SimpleGrantedAuthority("ROLE_ADMIN");
        Collection<? extends GrantedAuthority> authorities = Arrays.asList(authority1, authority2);
        doReturn(authorities).when(mockAuth).getAuthorities();

        when(mockAuth.isAuthenticated()).thenReturn(true);

        return mockAuth;
    }

    /**
     * This is used to create a mock {@link Authentication} object where most of the fields are null, to resolve
     * exceptions when optional fields are omitted.
     */
    private Authentication createNullMockAuthentication() {
        Authentication mockAuth = mock(Authentication.class);
        when(mockAuth.getPrincipal()).thenReturn("testuser");
        when(mockAuth.getCredentials()).thenReturn(null);
        when(mockAuth.getDetails()).thenReturn(null);

        doReturn(null).when(mockAuth).getAuthorities();

        when(mockAuth.isAuthenticated()).thenReturn(true);

        return mockAuth;
    }

    /**
     * Convert the value to JSON and then retrieve the value at the specified path.<br/>
     * Note that this does stringify all JSON types, including null, so there could be some slight shadowing problems.
     */
    private String jsonGet(Object root, String path) {
        JsonNode jsonRoot = this.mapper.valueToTree(root);
        return jsonRoot.at(path).asText();
    }

    /**
     * List all JSON paths found under the object.
     */
    private List<String> jsonList(Object root) {
        JsonNode jsonRoot = this.mapper.valueToTree(root);
        List<String> paths = new ArrayList<>();
        jsonList(jsonRoot, "", paths);
        return paths;
    }

    private void jsonList(JsonNode node, String currentPath, List<String> paths) {
        if (node.isValueNode()) {
            paths.add(currentPath);
        } else if (node.isObject()) {
            node.fields().forEachRemaining(entry -> {
                String fieldName = entry.getKey();
                JsonNode childNode = entry.getValue();
                jsonList(childNode, currentPath + "/" + fieldName, paths);
            });
        } else if (node.isArray()) {
            for (int i = 0; i < node.size(); i++) {
                jsonList(node.get(i), currentPath + "/" + i, paths);
            }
        }
    }

    /**
     * Create a human-readable list of differences between two objects. The possible paths are enumerated with
     * {@link #jsonList(Object)}, and they are retrieved using {@link #jsonGet(Object, String)}. This does mean that
     * all values are compared stringly.
     */
    private List<String> jsonDiff(Object rootA, Object rootB) {
        List<String> pathsA = jsonList(rootA);
        List<String> pathsB = jsonList(rootB);
        Set<String> pathSet = new HashSet<>(pathsA);
        pathSet.addAll(pathsB);
        List<String> paths = new ArrayList<>(pathSet);

        List<String> results = new ArrayList<>();

        for (int i = 0; i < paths.size(); i++) {
            String valA = jsonGet(rootA, paths.get(i));
            String valB = jsonGet(rootB, paths.get(i));
            if (!Objects.equals(valA, valB)) {
                results.add(String.format("%s: %s =/= %s", paths.get(i), valA, valB));
            }
        }

        return results;
    }

    private String jsonPretty(Object root) {
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
            //CHECKSTYLE:OFF
        } catch (Exception e) {
            //CHECKSTYLE:ON
            return String.format("failed to pretty print JSON: %s", e);
        }
    }

    @Nested
    @Import(OPAPathSelectorTest.CustomOPAConfig.class)
    public class OPAPathSelectorTest {

        @Autowired
        private OPAAuthorizationManager opaAuthorizationManager;

        @Test
        public void testOPAPathSelectorAlwaysTruePolicy() {
            Authentication mockAuth = createMockAuthentication();
            when(mockAuth.getPrincipal()).thenReturn("testuser_allowed");
            when(authenticationSupplier.get()).thenReturn(mockAuth);
            assertDoesNotThrow(() -> opaAuthorizationManager.verify(authenticationSupplier, context));
        }

        @Test
        public void testOPAPathSelectorAlwaysFalsePolicy() {
            Authentication mockAuth = createMockAuthentication();
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
}
