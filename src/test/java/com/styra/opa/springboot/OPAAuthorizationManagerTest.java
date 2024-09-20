package com.styra.opa.springboot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.styra.opa.OPAClient;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
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

import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Testcontainers
class OPAAuthorizationManagerTest {

    @Mock
    private Supplier<Authentication> authenticationSupplier;

    @Mock
    private RequestAuthorizationContext context;

    @Mock
    private HttpServletRequest httpServletRequest;

    private int opaPort = 8181;
    private int altPort = 8282;

    private ObjectMapper mapper = new ObjectMapper();

    // Checkstyle does not like magic numbers, but these are just test values.
    // The B value should be double the A value.
    private int testIntegerA = 8;
    private int testIntegerB = 16;
    private double testDoubleA = 3.14159;

    private String address;
    private String altAddress;
    private Map<String, String> headers = Map.ofEntries(entry("Authorization", "Bearer supersecret"));

    @Container
    // Checkstyle is disabled here because it wants opac to 'be private and
    // have accessor methods', which seems pointless and will probably mess up
    // test containers.
    //
    // Checkstyle also complains that this is in the wrong order, because public
    // variables are supposed to be declared first. But then it would need to
    // have magic numbers since opaPort and friends are private.
    //CHECKSTYLE:OFF
    public GenericContainer<?> opac = new GenericContainer<>(
            new ImageFromDockerfile()
                // .withFileFromClasspath(path_in_build_context, path_in_resources_dir)
                .withFileFromClasspath("Dockerfile", "opa.Dockerfile")
                .withFileFromClasspath("nginx.conf", "nginx.conf")
                .withFileFromClasspath("entrypoint.sh", "entrypoint.sh")
        )
        .withExposedPorts(opaPort, altPort)
        .withFileSystemBind("./testdata/simple", "/policy", BindMode.READ_ONLY)
        .withCommand("run -s --authentication=token --authorization=basic --bundle /policy");
    //CHECKSTYLE:ON

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
     * This is used to create a mock auth object where most of the fields are
     * null, to suss out exceptions when optional fields are omitted.
     *
     * @return
     */
    private Authentication createNullMockAuthentication() {
        Authentication mockAuth = mock(Authentication.class);
        when(mockAuth.getPrincipal()).thenReturn("testuser");
        when(mockAuth.getCredentials()).thenReturn(null);

        WebAuthenticationDetails details = new WebAuthenticationDetails(httpServletRequest);
        when(mockAuth.getDetails()).thenReturn(null);

        GrantedAuthority authority1 = new SimpleGrantedAuthority("ROLE_USER");
        GrantedAuthority authority2 = new SimpleGrantedAuthority("ROLE_ADMIN");
        Collection<? extends GrantedAuthority> authorities = Arrays.asList(authority1, authority2);
        doReturn(null).when(mockAuth).getAuthorities();

        when(mockAuth.isAuthenticated()).thenReturn(true);

        return mockAuth;
    }

    // Convert the value to JSON and then retrieve the value at the specified
    // path.
    //
    // Note that this does stringify all JSON types, including null, so there
    // could be some slight shadowing problems.
    private String jsonGet(Object root, String path) {
        JsonNode jroot = this.mapper.valueToTree(root);
        return jroot.at(path).asText();
    }

    // List all JSON paths found under the object.
    private List<String> jsonList(Object root) {
        JsonNode jroot = this.mapper.valueToTree(root);
        List<String> paths = new ArrayList<>();
        jsonList(jroot, "", paths);
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

    // Create a human-readable list of differences between to objects. The
    // possible paths are enumerated with jsonList(), and they are retrieved
    // using jsonGet(). This does mean that all values are compared stringily.
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
        //CHECKSTYLE:OFF
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
        } catch (Exception e) {
            return String.format("failed to pretty print JSON: %s", e);
        }
        //CHECKSTYLE:ON
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

        address = "http://" + opac.getHost() + ":" + opac.getMappedPort(opaPort);
        altAddress = "http://" + opac.getHost() + ":" + opac.getMappedPort(altPort) + "/customprefix";
    }

    @AfterEach
    public void dumpLogs() {
        System.out.println("==== container logs from OPA container ====");
        final String logs = opac.getLogs();
        System.out.println(logs);
    }

    @Test
    public void testOPAHealth() {
        // This test just makes sure that we can reach the OPAClient health endpoint
        // and that it returns the expected {} value.

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(address + "/health")).build();
        HttpResponse<String> resp = null;

        try {
            resp = client.send(req, HttpResponse.BodyHandlers.ofString());

        //CHECKSTYLE:OFF
        } catch (Exception e) {
            //CHECKSTYLE:ON
            System.out.println("exception: " + e);
            assertNull(e);
        }

        String body = resp.body();

        assertEquals("{}\n", body);
    }

    @Test
    public void testOPAHealthAlternate() {
        // This makes sure that we can also successfully reach the OPA health
        // API on the "alternate", reverse-proxy based OPA that has a URL
        // prefix.

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(altAddress + "/health")).build();
        HttpResponse<String> resp = null;

        try {
            resp = client.send(req, HttpResponse.BodyHandlers.ofString());

        //CHECKSTYLE:OFF
        } catch (Exception e) {
            //CHECKSTYLE:ON
            System.out.println("exception: " + e);
            assertNull(e);
        }

        String body = resp.body();

        assertEquals("{}\n", body);
    }

    @Test
    public void testOPAAuthorizationManagerSimpleAllow() {
        // Make sure that with a simple always-allow rule, we allow all
        // requests.

        Authentication mockAuth = this.createMockAuthentication();
        when(authenticationSupplier.get()).thenReturn(mockAuth);
        OPAClient opa = new OPAClient(address, headers);
        AuthorizationManager<RequestAuthorizationContext> am =
            new OPAAuthorizationManager(opa, "policy/decision_always_true");
        AuthorizationDecision actual = am.check(this.authenticationSupplier, this.context);
        assertEquals(actual.isGranted(), true);

    }

    @Test
    public void testOPAAuthorizationManagerSimpleDeny() {
        // Make sure that with a simple always-deny rule, we deny all
        // requests.

        Authentication mockAuth = this.createMockAuthentication();
        when(authenticationSupplier.get()).thenReturn(mockAuth);
        OPAClient opa = new OPAClient(address, headers);
        AuthorizationManager<RequestAuthorizationContext> am =
            new OPAAuthorizationManager(opa, "policy/decision_always_false");
        AuthorizationDecision actual = am.check(this.authenticationSupplier, this.context);
        assertEquals(actual.isGranted(), false);

    }

    @Test
    public void testOPAAuthorizationManagerSimpleAllowVerify() {
        // Make sure that with a simple always-allow rule, we allow all
        // requests, using .verify().

        Authentication mockAuth = this.createMockAuthentication();
        when(authenticationSupplier.get()).thenReturn(mockAuth);
        OPAClient opa = new OPAClient(address, headers);
        AuthorizationManager<RequestAuthorizationContext> am =
            new OPAAuthorizationManager(opa, "policy/decision_always_true");
        am.verify(this.authenticationSupplier, this.context);

    }

    @Test
    public void testOPAAuthorizationManagerSimpleDenyVerify() {
        // Make sure that with a simple always-deny rule, we deny all
        // requests, using .verify()

        Authentication mockAuth = this.createMockAuthentication();
        when(authenticationSupplier.get()).thenReturn(mockAuth);
        OPAClient opa = new OPAClient(address, headers);
        AuthorizationManager<RequestAuthorizationContext> am =
            new OPAAuthorizationManager(opa, "policy/decision_always_false");

        assertThrows(AccessDeniedException.class, () -> {
            am.verify(this.authenticationSupplier, this.context);
        });

    }

    @Test
    public void testOPAAuthorizationManagerEcho() {
        // By reading back the input, we can make sure the OPA input has the
        // right structure and content.

        Map<String, Object> expectData = Map.ofEntries(
                entry("action", Map.ofEntries(
                    entry("headers", Map.ofEntries(
                        entry("UnitTestHeader", "123abc")
                    )),
                    entry("name", "GET"),
                    entry("protocol", "HTTP/1.1")
                )),
                entry("context", Map.ofEntries(
                    entry("host", "example.com"),
                    entry("ip", "192.0.2.123"),
                    entry("port", 0),
                    entry("type", "http"),
                    entry("data", Map.ofEntries(
                        entry("hello", "world")
                    ))
                )),
                entry("resource", Map.ofEntries(
                    entry("id", "unit/test"),
                    entry("type", "endpoint")
                )),
                entry("subject", Map.ofEntries(
                    entry("authorities", List.of(
                        Map.ofEntries(entry("authority", "ROLE_USER")),
                        Map.ofEntries(entry("authority", "ROLE_ADMIN"))
                    )),
                    entry("details", Map.ofEntries(
                        entry("remoteAddress", "192.0.2.123"),
                        entry("sessionId", "null")
                    )),
                    entry("id", "testuser"),
                    entry("type", "java_authentication")
                ))
        );

        OPAResponseContext expectCtx = new OPAResponseContext();
        expectCtx.setReasonUser(Map.ofEntries(
            entry("en", "echo rule always allows"),
            entry("other", "other reason key")
        ));
        expectCtx.setId("0");
        expectCtx.setData(expectData);

        OPAResponse expect = new OPAResponse();
        expect.setDecision(true);
        expect.setContext(expectCtx);

        ContextDataProvider prov = new ConstantContextDataProvider(java.util.Map.ofEntries(
            entry("hello", "world")
        ));
        Authentication mockAuth = this.createMockAuthentication();
        when(authenticationSupplier.get()).thenReturn(mockAuth);
        OPAClient opa = new OPAClient(address, headers);
        OPAAuthorizationManager am = new OPAAuthorizationManager(opa, "policy/echo", prov);
        OPAResponse actual = am.opaRequest(this.authenticationSupplier, this.context);

        assertEquals(expect.getDecision(), actual.getDecision());
        assertEquals(expect.getContext().getId(), actual.getContext().getId());
        assertEquals(expect.getContext().getReasonUser(), actual.getContext().getReasonUser());

        List<String> datadiff = jsonDiff(expect.getContext().getData(), actual.getContext().getData());

        System.out.printf("#### expected context data\n%s\n", jsonPretty(expectData));
        System.out.printf("#### actual context data\n%s\n", jsonPretty(actual.getContext().getData()));

        for (int i = 0; i < datadiff.size(); i++) {
            System.out.printf("diff mismatch: %s\n", datadiff.get(i));
        }

        assertEquals(0, datadiff.size());

        assertEquals("echo rule always allows", actual.getReasonForDecision("en"));
        assertEquals("other reason key", actual.getReasonForDecision("other"));
        assertEquals("echo rule always allows", actual.getReasonForDecision("nonexistant"));

    }

    @Test
    public void testOPAAuthorizationManagerNullMetadata() {
        // By reading back the input, we can make sure the OPA input has the
        // right structure and content.

        Map<String, Object> expectData = Map.ofEntries(
                entry("action", Map.ofEntries(
                    entry("headers", Map.ofEntries(
                        entry("UnitTestHeader", "123abc")
                    )),
                    entry("name", "GET"),
                    entry("protocol", "HTTP/1.1")
                )),
                entry("context", Map.ofEntries(
                    entry("host", "example.com"),
                    entry("ip", "192.0.2.123"),
                    entry("port", 0),
                    entry("type", "http"),
                    entry("data", Map.ofEntries(
                        entry("hello", "world")
                    ))
                )),
                entry("resource", Map.ofEntries(
                    entry("id", "unit/test"),
                    entry("type", "endpoint")
                )),
                entry("subject", Map.ofEntries(
                    entry("id", "testuser"),
                    entry("type", "java_authentication")
                ))
        );

        OPAResponseContext expectCtx = new OPAResponseContext();
        expectCtx.setReasonUser(Map.ofEntries(
            entry("en", "echo rule always allows"),
            entry("other", "other reason key")
        ));
        expectCtx.setId("0");
        expectCtx.setData(expectData);

        OPAResponse expect = new OPAResponse();
        expect.setDecision(true);
        expect.setContext(expectCtx);

        ContextDataProvider prov = new ConstantContextDataProvider(java.util.Map.ofEntries(
            entry("hello", "world")
        ));
        Authentication mockAuth = this.createNullMockAuthentication();
        when(authenticationSupplier.get()).thenReturn(mockAuth);
        OPAClient opa = new OPAClient(address, headers);
        OPAAuthorizationManager am = new OPAAuthorizationManager(opa, "policy/echo", prov);
        OPAResponse actual = am.opaRequest(this.authenticationSupplier, this.context);

        assertEquals(expect.getDecision(), actual.getDecision());
        assertEquals(expect.getContext().getId(), actual.getContext().getId());
        assertEquals(expect.getContext().getReasonUser(), actual.getContext().getReasonUser());

        List<String> datadiff = jsonDiff(expect.getContext().getData(), actual.getContext().getData());

        System.out.printf("#### expected context data\n%s\n", jsonPretty(expectData));
        System.out.printf("#### actual context data\n%s\n", jsonPretty(actual.getContext().getData()));

        for (int i = 0; i < datadiff.size(); i++) {
            System.out.printf("diff mismatch: %s\n", datadiff.get(i));
        }

        assertEquals(0, datadiff.size());

        assertEquals("echo rule always allows", actual.getReasonForDecision("en"));
        assertEquals("other reason key", actual.getReasonForDecision("other"));
        assertEquals("echo rule always allows", actual.getReasonForDecision("nonexistant"));

    }

}
