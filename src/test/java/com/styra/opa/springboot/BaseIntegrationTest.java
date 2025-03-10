package com.styra.opa.springboot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.styra.opa.springboot.autoconfigure.OPAAutoConfiguration;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = OPAAutoConfiguration.class)
@Testcontainers
public abstract class BaseIntegrationTest {
    protected static final int OPA_PORT = 8181;
    /**
     * Besides OPA default port ({@code opaPort}), OPA is exposed under {@code localhost:8282/customprefix} using
     * Nginx.<br/>
     * Nginx configuration is provided in {@code src/test/resources/nginx.conf} file.
     */
    protected static final int ALT_PORT = 8282;
    protected static final Map<String, String> HEADERS = Map.ofEntries(entry("Authorization", "Bearer supersecret"));

    //CHECKSTYLE:OFF
    /*
     * Checkstyle is disabled here to allow protected fields. Morover, Checkstyle wants opaContainer to 'be private and
     * have accessor methods', which seems pointless and will probably mess up test containers.
     */
    @Mock
    protected Supplier<Authentication> authenticationSupplier;
    @Mock
    protected RequestAuthorizationContext context;
    @Mock
    protected HttpServletRequest httpServletRequest;

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

    protected final ObjectMapper objectMapper = new ObjectMapper();
    protected String address;
    protected String altAddress;
    //CHECKSTYLE:ON

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

    protected Authentication createMockAuthentication() {
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
    protected Authentication createNullMockAuthentication() {
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
    protected String jsonGet(Object root, String path) {
        JsonNode jsonRoot = this.objectMapper.valueToTree(root);
        return jsonRoot.at(path).asText();
    }

    /**
     * List all JSON paths found under the object.
     */
    protected List<String> jsonList(Object root) {
        JsonNode jsonRoot = this.objectMapper.valueToTree(root);
        List<String> paths = new ArrayList<>();
        jsonList(jsonRoot, "", paths);
        return paths;
    }

    protected void jsonList(JsonNode node, String currentPath, List<String> paths) {
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
    protected List<String> jsonDiff(Object rootA, Object rootB) {
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

    protected String jsonPretty(Object root) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
            //CHECKSTYLE:OFF
        } catch (Exception e) {
            //CHECKSTYLE:ON
            return String.format("failed to pretty print JSON: %s", e);
        }
    }
}
