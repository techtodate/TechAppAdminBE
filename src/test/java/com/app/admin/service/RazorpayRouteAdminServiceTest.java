package com.app.admin.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.app.admin.security.AdminSecurityContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class RazorpayRouteAdminServiceTest {
    private static final String KEY_ID = "rzp_test_readiness_key";
    private final JdbcTemplate database = mock(JdbcTemplate.class);
    private final AdminSecurityContext security = mock(AdminSecurityContext.class);
    private MockRestServiceServer paymentServer;
    private MockRestServiceServer routeServer;
    private RazorpayRouteAdminService service;

    @BeforeEach
    void setUp() {
        RestClient.Builder paymentBuilder = RestClient.builder().baseUrl("https://api.razorpay.com/v1");
        RestClient.Builder routeBuilder = RestClient.builder().baseUrl("https://api.razorpay.com/v2");
        paymentServer = MockRestServiceServer.bindTo(paymentBuilder).build();
        routeServer = MockRestServiceServer.bindTo(routeBuilder).build();
        service = new RazorpayRouteAdminService(KEY_ID, "secret-for-test", "TEST", false, false,
                database, security, paymentBuilder.build(), routeBuilder.build());
    }

    @AfterEach
    void verifyProviderRequests() {
        paymentServer.verify();
        routeServer.verify();
    }

    @Test
    void paymentApiAuthenticationUsesReadOnlyPaymentsEndpointAndDoesNotClaimRouteActivation() {
        paymentServer.expect(requestTo("https://api.razorpay.com/v1/payments?count=1"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"entity\":\"collection\",\"count\":0,\"items\":[]}", MediaType.APPLICATION_JSON));

        var result = service.checkConnection();

        assertEquals("VERIFIED", result.get("status"));
        assertEquals("GET", result.get("method"));
        assertEquals("/v1/payments?count=1", result.get("endpoint"));
        assertEquals("TEST", result.get("environment"));
    }

    @Test
    void linkedAccountLookupVerifiesOnlyTheSpecificRouteAccount() {
        routeServer.expect(requestTo("https://api.razorpay.com/v2/accounts/acc_test_sample"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"id\":\"acc_test_sample\",\"type\":\"route\",\"status\":\"created\",\"email\":\"private@example.test\",\"product_config\":{\"activation_status\":\"pending\"}}", MediaType.APPLICATION_JSON));
        when(database.update(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.<Object>any(),
                org.mockito.ArgumentMatchers.<Object>any(), org.mockito.ArgumentMatchers.<Object>any(),
                org.mockito.ArgumentMatchers.<Object>any(), org.mockito.ArgumentMatchers.<Object>any())).thenReturn(0);

        var result = service.checkLinkedAccount("acc_test_sample");

        assertEquals("VERIFIED", result.get("status"));
        assertEquals("/v2/accounts/{account_id}", result.get("endpoint"));
        assertEquals("PENDING", result.get("activationStatus"));
        @SuppressWarnings("unchecked") var safeAccount = (java.util.Map<String,Object>) result.get("account");
        assertEquals("created", safeAccount.get("status"));
        assertFalse(safeAccount.containsKey("email"));
    }

    @Test
    void routePermissionFailureIsReportedAsRestrictedWithSanitizedProviderCode() {
        routeServer.expect(requestTo("https://api.razorpay.com/v2/accounts/acc_test_sample"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.FORBIDDEN).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":{\"code\":\"FEATURE_NOT_ENABLED\",\"description\":\"do not expose details\"}}"));

        var result = service.checkLinkedAccount("acc_test_sample");

        assertEquals("RESTRICTED", result.get("status"));
        assertEquals(403, result.get("httpStatus"));
        assertEquals("FEATURE_NOT_ENABLED", result.get("providerErrorCode"));
        assertFalse(String.valueOf(result.get("explanation")).contains("do not expose details"));
    }
}
