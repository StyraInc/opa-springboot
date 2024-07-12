package com.styra.opa.springboot;

import com.styra.opa.OPAClient;
import com.styra.opa.OPAException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@Testcontainers
class OPAAuthorizationManagerTest {

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

    @BeforeEach
    public void setUp() {
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

        OPAClient opa = new OPAClient(address, headers);
        AuthorizationManager<RequestAuthorizationContext> am = new OPAAuthorizationManager(opa);

    }

}
