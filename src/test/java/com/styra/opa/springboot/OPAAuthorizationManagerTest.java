package com.styra.opa.springboot;

import com.styra.opa.OPAClient;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static com.styra.opa.springboot.input.InputConstants.SUBJECT;
import static com.styra.opa.springboot.input.InputConstants.SUBJECT_AUTHORITIES;
import static com.styra.opa.springboot.input.InputConstants.SUBJECT_DETAILS;
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
        var client = HttpClient.newHttpClient();
        var request = HttpRequest.newBuilder().uri(URI.create(address + "/health")).build();
        HttpResponse<String> response = null;

        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
            //CHECKSTYLE:OFF
        } catch (Exception e) {
            //CHECKSTYLE:ON
            System.out.println("exception: " + e);
            assertNull(e);
        }

        var responseBody = response.body();
        assertEquals("{}\n", responseBody);
    }

    /**
     * This makes sure that we can also successfully reach the OPA health API on the "alternate", reverse-proxy based
     * OPA that has a URL prefix.
     */
    @Test
    public void testOPAHealthAlternate() {
        var client = HttpClient.newHttpClient();
        var request = HttpRequest.newBuilder().uri(URI.create(altAddress + "/health")).build();
        HttpResponse<String> response = null;

        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
            //CHECKSTYLE:OFF
        } catch (Exception e) {
            //CHECKSTYLE:ON
            System.out.println("exception: " + e);
            assertNull(e);
        }

        var responseBody = response.body();
        assertEquals("{}\n", responseBody);
    }

    /**
     * Make sure that with a simple always-allow rule, we allow all requests, using
     * {@link OPAAuthorizationManager#check(Supplier, RequestAuthorizationContext)}.
     */
    @Test
    public void testOPAAuthorizationManagerSimpleAllow() {
        var mockAuth = createMockAuthentication();
        when(authenticationSupplier.get()).thenReturn(mockAuth);
        var opaClient = new OPAClient(address, HEADERS);
        var opaAuthorizationManager = new OPAAuthorizationManager(opaClient, "policy/decision_always_true");
        var actual = opaAuthorizationManager.check(authenticationSupplier, context);
        assertTrue(actual.isGranted());
    }

    /**
     * Make sure that with a simple always-deny rule, we deny all requests, using
     * {@link OPAAuthorizationManager#check(Supplier, RequestAuthorizationContext)}.
     */
    @Test
    public void testOPAAuthorizationManagerSimpleDeny() {
        var mockAuth = createMockAuthentication();
        when(authenticationSupplier.get()).thenReturn(mockAuth);
        var opaClient = new OPAClient(address, HEADERS);
        var opaAuthorizationManager = new OPAAuthorizationManager(opaClient, "policy/decision_always_false");
        var actual = opaAuthorizationManager.check(authenticationSupplier, context);
        assertFalse(actual.isGranted());
    }

    /**
     * Make sure that with a simple always-allow rule, we allow all requests, using
     * {@link OPAAuthorizationManager#verify(Supplier, RequestAuthorizationContext)}.
     */
    @Test
    public void testOPAAuthorizationManagerSimpleAllowVerify() {
        var mockAuth = createMockAuthentication();
        when(authenticationSupplier.get()).thenReturn(mockAuth);
        var opaClient = new OPAClient(address, HEADERS);
        var opaAuthorizationManager = new OPAAuthorizationManager(opaClient, "policy/decision_always_true");
        assertDoesNotThrow(() -> opaAuthorizationManager.verify(authenticationSupplier, context));
    }

    /**
     * Make sure that with a simple always-deny rule, we deny all requests, using
     * {@link OPAAuthorizationManager#verify(Supplier, RequestAuthorizationContext)}.
     */
    @Test
    public void testOPAAuthorizationManagerSimpleDenyVerify() {
        var mockAuth = createMockAuthentication();
        when(authenticationSupplier.get()).thenReturn(mockAuth);
        var opaClient = new OPAClient(address, HEADERS);
        var opaAuthorizationManager = new OPAAuthorizationManager(opaClient, "policy/decision_always_false");
        assertThrows(AccessDeniedException.class,
            () -> opaAuthorizationManager.verify(authenticationSupplier, context));
    }

    /**
     * By reading back the input, we can make sure the OPA input has the right structure and content.
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testOPAAuthorizationManagerEcho() {
        Map<String, Object> expectedResponseContextData = new HashMap<>(createNullMockAuthOPAInput());
        Map<String, Object> subject = new HashMap<>((Map<String, Object>) expectedResponseContextData.get(SUBJECT));
        subject.putAll(Map.ofEntries(
            entry(SUBJECT_AUTHORITIES, List.of(
                Map.ofEntries(entry("authority", "ROLE_USER")),
                Map.ofEntries(entry("authority", "ROLE_ADMIN"))
            )),
            entry(SUBJECT_DETAILS, Map.ofEntries(
                entry("remoteAddress", "192.0.2.123"),
                entry("sessionId", "null")
            ))
        ));
        expectedResponseContextData.put(SUBJECT, subject);

        var expectedResponseContext = new OPAResponseContext();
        expectedResponseContext.setReasonUser(Map.ofEntries(
            entry("en", "echo rule always allows"),
            entry("other", "other reason key")
        ));
        expectedResponseContext.setId("0");
        expectedResponseContext.setData(expectedResponseContextData);

        var expectedResponse = new OPAResponse();
        expectedResponse.setDecision(true);
        expectedResponse.setContext(expectedResponseContext);

        var contextDataProvider = new ConstantContextDataProvider(Map.ofEntries(
            entry("hello", "world")
        ));
        var mockAuth = createMockAuthentication();
        when(authenticationSupplier.get()).thenReturn(mockAuth);
        var opaClient = new OPAClient(address, HEADERS);
        var opaAuthorizationManager = new OPAAuthorizationManager(opaClient, "policy/echo", contextDataProvider);
        var actualResponse = opaAuthorizationManager.opaRequest(authenticationSupplier, context);

        assertEquals(expectedResponse.getDecision(), actualResponse.getDecision());
        assertEquals(expectedResponse.getContext().getId(), actualResponse.getContext().getId());
        assertEquals(expectedResponse.getContext().getReasonUser(), actualResponse.getContext().getReasonUser());

        var dataDiffs = jsonDiff(expectedResponse.getContext().getData(), actualResponse.getContext().getData());

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
        Map<String, Object> expectedResponseContextData = createNullMockAuthOPAInput();

        var expectedResponseContext = new OPAResponseContext();
        expectedResponseContext.setReasonUser(Map.ofEntries(
            entry("en", "echo rule always allows"),
            entry("other", "other reason key")
        ));
        expectedResponseContext.setId("0");
        expectedResponseContext.setData(expectedResponseContextData);

        var expectedResponse = new OPAResponse();
        expectedResponse.setDecision(true);
        expectedResponse.setContext(expectedResponseContext);

        var contextDataProvider = new ConstantContextDataProvider(Map.ofEntries(
            entry("hello", "world")
        ));
        var mockAuth = createNullMockAuthentication();
        when(authenticationSupplier.get()).thenReturn(mockAuth);
        var opaClient = new OPAClient(address, HEADERS);
        var opaAuthorizationManager = new OPAAuthorizationManager(opaClient, "policy/echo", contextDataProvider);
        var actualResponse = opaAuthorizationManager.opaRequest(authenticationSupplier, context);

        assertEquals(expectedResponse.getDecision(), actualResponse.getDecision());
        assertEquals(expectedResponse.getContext().getId(), actualResponse.getContext().getId());
        assertEquals(expectedResponse.getContext().getReasonUser(), actualResponse.getContext().getReasonUser());

        var dataDiffs = jsonDiff(expectedResponse.getContext().getData(),
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
