package com.styra.opa.springboot;

import com.styra.opa.OPAClient;
import com.styra.opa.springboot.autoconfigure.OPAProperties;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
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
import static org.mockito.Mockito.when;

class OPAAuthorizationManagerTest extends BaseIntegrationTest {

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
}
