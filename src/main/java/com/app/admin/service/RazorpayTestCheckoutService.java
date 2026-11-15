package com.app.admin.service;

import com.app.admin.dto.RazorpayTestOrderRequest;
import com.app.admin.dto.RazorpayTestPaymentVerificationRequest;
import com.app.admin.security.AdminSecurityContext;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RazorpayTestCheckoutService {
    private final String keyId;
    private final String keySecret;
    private final AdminSecurityContext security;
    private final RazorpaySignatureVerifier signatures;
    private final RestClient razorpay = RestClient.builder().baseUrl("https://api.razorpay.com/v1").build();

    public RazorpayTestCheckoutService(@Value("${razorpay.key-id:}") String keyId,
            @Value("${razorpay.key-secret:}") String keySecret, AdminSecurityContext security,
            RazorpaySignatureVerifier signatures) {
        this.keyId = keyId;
        this.keySecret = keySecret;
        this.security = security;
        this.signatures = signatures;
    }

    public Map<String, Object> createOrder(RazorpayTestOrderRequest request) {
        security.requirePermission(AdminSecurityContext.PERMISSION_FINANCIAL_MANAGE);
        requireTestCredentials();
        if (request.amount() < 100) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Amount must be at least 100 paise.");
        if (!"INR".equalsIgnoreCase(request.currency())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Test checkout currently supports INR only.");
        String receipt = "test-" + UUID.randomUUID().toString().replace("-", "");
        try {
            Map<?, ?> response = razorpay.post().uri("/orders")
                    .headers(headers -> headers.setBasicAuth(keyId, keySecret))
                    .body(Map.of("amount", request.amount(), "currency", "INR", "receipt", receipt))
                    .retrieve().body(Map.class);
            if (response == null || response.get("id") == null) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Razorpay returned an invalid order response.");
            }
            return Map.of("order_id", response.get("id"), "amount", response.get("amount"), "currency", response.get("currency"));
        } catch (RestClientResponseException exception) {
            HttpStatus status = exception.getStatusCode().value() == 401 ? HttpStatus.UNAUTHORIZED : HttpStatus.BAD_GATEWAY;
            throw new ResponseStatusException(status, "Unable to create a Razorpay test order.");
        }
    }

    public Map<String, Object> verifyPayment(RazorpayTestPaymentVerificationRequest request) {
        security.requirePermission(AdminSecurityContext.PERMISSION_FINANCIAL_MANAGE);
        requireTestCredentials();
        signatures.verify(request.razorpayOrderId(), request.razorpayPaymentId(), request.razorpaySignature(), keySecret);
        try {
            Map<?, ?> order = razorpay.get().uri("/orders/{orderId}", request.razorpayOrderId())
                    .headers(headers -> headers.setBasicAuth(keyId, keySecret)).retrieve().body(Map.class);
            Map<?, ?> payment = razorpay.get().uri("/payments/{paymentId}", request.razorpayPaymentId())
                    .headers(headers -> headers.setBasicAuth(keyId, keySecret)).retrieve().body(Map.class);
            if (order == null || payment == null || !request.razorpayOrderId().equals(payment.get("order_id"))) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment does not belong to this Razorpay order.");
            }
            String paymentStatus = String.valueOf(payment.get("status"));
            if (!"authorized".equalsIgnoreCase(paymentStatus) && !"captured".equalsIgnoreCase(paymentStatus)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Razorpay has not confirmed a successful payment.");
            }
            if (!String.valueOf(order.get("amount")).equals(String.valueOf(payment.get("amount")))
                    || !String.valueOf(order.get("currency")).equalsIgnoreCase(String.valueOf(payment.get("currency")))) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment amount or currency does not match the Razorpay order.");
            }
            return Map.of("verified", true, "test_mode", true, "status", payment.get("status"),
                    "razorpay_payment_id", request.razorpayPaymentId(), "razorpay_order_id", request.razorpayOrderId());
        } catch (RestClientResponseException exception) {
            HttpStatus status = exception.getStatusCode().value() == 401 ? HttpStatus.UNAUTHORIZED : HttpStatus.BAD_GATEWAY;
            throw new ResponseStatusException(status, "Unable to confirm the test payment with Razorpay.");
        }
    }

    private void requireTestCredentials() {
        if (keyId.isBlank() || keySecret.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Razorpay is not configured.");
        }
        if (!keyId.startsWith("rzp_test_")) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "The sample checkout screen is disabled unless Razorpay test credentials are configured.");
        }
    }
}
