package com.styra.opa.springboot;

import com.styra.opa.OPAClient;
import com.styra.opa.OPAException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.TestingAuthenticationToken;
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
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doReturn;

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
        // This is a unit test, I will catch whatever exceptions I want.
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
        // This is a unit test, I will catch whatever exceptions I want.
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
        AuthorizationManager<RequestAuthorizationContext> am = new OPAAuthorizationManager(opa, "policy/decision_always_true");
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
        AuthorizationManager<RequestAuthorizationContext> am = new OPAAuthorizationManager(opa, "policy/decision_always_false");
        AuthorizationDecision actual = am.check(this.authenticationSupplier, this.context);
        assertEquals(actual.isGranted(), false);

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
                    entry("type", "http")
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
        expectCtx.setReasonUser(Map.ofEntries(entry("en", "echo rule always allows")));
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

        assertEquals(actual.getDecision(), expect.getDecision());
        assertEquals(actual.getContext().getId(), expect.getContext().getId());
        assertEquals(actual.getContext().getReasonUser(), expect.getContext().getReasonUser());
        assertEquals(actual.getContext().getData(), expect.getContext().getData());

    }

}
