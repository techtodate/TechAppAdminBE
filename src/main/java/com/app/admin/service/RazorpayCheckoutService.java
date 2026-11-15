package com.app.admin.service;

import com.app.admin.dto.RazorpayOrderRequest;
import com.app.admin.dto.RazorpayPaymentVerificationRequest;
import com.app.admin.security.AdminSecurityContext;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RazorpayCheckoutService {
    private final String keyId;
    private final String keySecret;
    private final JdbcTemplate db;
    private final AdminSecurityContext security;
    private final RazorpaySignatureVerifier signatures;
    private final RestClient client = RestClient.builder().baseUrl("https://api.razorpay.com/v1").build();

    public RazorpayCheckoutService(@Value("${razorpay.key-id:}") String keyId,
            @Value("${razorpay.key-secret:}") String keySecret, JdbcTemplate db, AdminSecurityContext security, RazorpaySignatureVerifier signatures) {
        this.keyId = keyId;
        this.keySecret = keySecret;
        this.db = db;
        this.security = security;
        this.signatures = signatures;
    }

    @Transactional
    public Map<String, Object> createOrder(RazorpayOrderRequest request) {
        security.requirePermission(AdminSecurityContext.PERMISSION_FINANCIAL_MANAGE);
        requireConfigured();
        List<Map<String, Object>> rows = db.queryForList("""
                SELECT id,enrollment_id,trainee_user_id,trainer_user_id,amount,currency,payment_provider_id,
                       provider_order_id,status
                  FROM training_payment_orders
                 WHERE enrollment_id=? AND status IN ('CREATED','ATTEMPTED')
                 ORDER BY id DESC LIMIT 1 FOR UPDATE
                """, request.enrollmentId());
        if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.CONFLICT, "No unpaid payment order exists for this enrollment.");
        Map<String, Object> order = rows.get(0);
        BigDecimal amount = (BigDecimal) order.get("amount");
        if (amount == null || amount.movePointRight(2).longValueExact() < 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment order amount must be at least 100 paise.");
        }
        long amountPaise = amount.movePointRight(2).longValueExact();
        String currency = String.valueOf(order.get("currency"));
        if (!"INR".equalsIgnoreCase(currency)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only INR payment orders are supported.");
        Long paymentProviderId = order.get("payment_provider_id") == null ? null : ((Number) order.get("payment_provider_id")).longValue();
        if (paymentProviderId != null) {
            String providerCode = db.queryForObject("SELECT code FROM payment_providers WHERE id=?", String.class, paymentProviderId);
            if (!"RAZORPAY".equalsIgnoreCase(providerCode)) throw new ResponseStatusException(HttpStatus.CONFLICT, "The payment order is assigned to a different provider.");
        } else {
            List<Long> providers = db.query("SELECT id FROM payment_providers WHERE code='RAZORPAY' AND active=TRUE", (row, index) -> row.getLong("id"));
            if (providers.isEmpty()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "No active Razorpay payment provider is configured.");
            paymentProviderId = providers.get(0);
        }
        if (order.get("payment_provider_id") == null) {
            db.update("UPDATE training_payment_orders SET payment_provider_id=? WHERE id=?", paymentProviderId, order.get("id"));
            order.put("payment_provider_id", paymentProviderId);
        }
        String existingProviderOrderId = (String) order.get("provider_order_id");
        if (existingProviderOrderId != null && !existingProviderOrderId.isBlank()) {
            return Map.of("order_id", existingProviderOrderId, "amount", amountPaise, "currency", currency);
        }
        try {
            Map<?, ?> response = client.post().uri("/orders")
                    .headers(headers -> headers.setBasicAuth(keyId, keySecret))
                    .body(Map.of("amount", amountPaise, "currency", currency,
                            "receipt", "training-order-" + order.get("id")))
                    .retrieve().body(Map.class);
            if (response == null || response.get("id") == null) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Razorpay returned an invalid order response.");
            }
            String providerOrderId = String.valueOf(response.get("id"));
            db.update("""
                    UPDATE training_payment_orders
                       SET provider_order_id=?,payment_provider_id=?,status='ATTEMPTED',updated_at=CURRENT_TIMESTAMP
                     WHERE id=?
                    """, providerOrderId, paymentProviderId, order.get("id"));
            return Map.of("order_id", providerOrderId, "amount", amountPaise, "currency", currency);
        } catch (RestClientResponseException exception) {
            HttpStatus status = exception.getStatusCode().value() == 401 ? HttpStatus.UNAUTHORIZED : HttpStatus.INTERNAL_SERVER_ERROR;
            throw new ResponseStatusException(status, "Unable to create the Razorpay order.");
        }
    }

    @Transactional
    public Map<String, Object> verifyPayment(RazorpayPaymentVerificationRequest request) {
        security.requirePermission(AdminSecurityContext.PERMISSION_FINANCIAL_MANAGE);
        requireConfigured();
        signatures.verify(request.razorpayOrderId(), request.razorpayPaymentId(), request.razorpaySignature(), keySecret);
        List<Map<String, Object>> orders = db.queryForList("""
                SELECT id,enrollment_id,trainee_user_id,trainer_user_id,amount,currency,payment_provider_id
                  FROM training_payment_orders WHERE provider_order_id=? FOR UPDATE
                """, request.razorpayOrderId());
        if (orders.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Razorpay order is not associated with a training payment order.");
        Map<String, Object> order = orders.get(0);
        Map<?, ?> providerPayment = providerPayment(request.razorpayPaymentId());
        if (!request.razorpayOrderId().equals(providerPayment.get("order_id"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment does not belong to the verified order.");
        }
        BigDecimal expectedAmount = (BigDecimal) order.get("amount");
        BigDecimal paidAmount = new BigDecimal(String.valueOf(providerPayment.get("amount"))).movePointLeft(2);
        String currency = String.valueOf(order.get("currency"));
        if (expectedAmount.compareTo(paidAmount) != 0 || !currency.equalsIgnoreCase(String.valueOf(providerPayment.get("currency")))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment amount or currency does not match the payment order.");
        }
        String status = String.valueOf(providerPayment.get("status"));
        if ("authorized".equalsIgnoreCase(status)) {
            providerPayment = capturePayment(request.razorpayPaymentId(), expectedAmount.movePointRight(2).longValueExact(), currency);
            status = String.valueOf(providerPayment.get("status"));
        }
        if (!"captured".equalsIgnoreCase(status)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Razorpay has not captured this payment.");
        }
        Long localPaymentId = persistCapturedPayment(order, request.razorpayPaymentId(), request.razorpayOrderId(),
                paidAmount, currency, providerPayment.get("method"));
        return Map.of("verified", true, "status", "CAPTURED", "payment_id", localPaymentId,
                "razorpay_payment_id", request.razorpayPaymentId(), "razorpay_order_id", request.razorpayOrderId());
    }

    private Map<?, ?> providerPayment(String paymentId) {
        try {
            Map<?, ?> response = client.get().uri("/payments/{paymentId}", paymentId)
                    .headers(headers -> headers.setBasicAuth(keyId, keySecret)).retrieve().body(Map.class);
            if (response == null) throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Razorpay returned an invalid payment response.");
            return response;
        } catch (RestClientResponseException exception) {
            HttpStatus status = exception.getStatusCode().value() == 401 ? HttpStatus.UNAUTHORIZED : HttpStatus.BAD_GATEWAY;
            throw new ResponseStatusException(status, "Unable to confirm payment with Razorpay.");
        }
    }

    private Map<?, ?> capturePayment(String paymentId, long amountPaise, String currency) {
        try {
            Map<?, ?> response = client.post().uri("/payments/{paymentId}/capture", paymentId)
                    .headers(headers -> headers.setBasicAuth(keyId, keySecret))
                    .body(Map.of("amount", amountPaise, "currency", currency)).retrieve().body(Map.class);
            if (response == null) throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Razorpay returned an invalid capture response.");
            return response;
        } catch (RestClientResponseException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Razorpay could not capture the payment.");
        }
    }

    private Long persistCapturedPayment(Map<String, Object> order, String paymentId, String providerOrderId,
            BigDecimal amount, String currency, Object method) {
        Long providerId = ((Number) order.get("payment_provider_id")).longValue();
        List<Long> paymentIds = db.query("""
                INSERT INTO training_payments(payment_order_id,enrollment_id,trainee_user_id,trainer_user_id,
                    payment_provider_id,provider_payment_id,provider_order_id,amount,currency,payment_method,status,captured_at)
                VALUES (?,?,?,?,?,?,?,?,?,?, 'CAPTURED',CURRENT_TIMESTAMP)
                ON CONFLICT (provider_payment_id) DO UPDATE
                    SET status='CAPTURED',captured_at=COALESCE(training_payments.captured_at,CURRENT_TIMESTAMP)
                  WHERE training_payments.payment_order_id=EXCLUDED.payment_order_id
                RETURNING id
                """, (row, index) -> row.getLong("id"), order.get("id"), order.get("enrollment_id"), order.get("trainee_user_id"),
                order.get("trainer_user_id"), providerId, paymentId, providerOrderId, amount, currency, method);
        if (paymentIds.isEmpty()) throw new ResponseStatusException(HttpStatus.CONFLICT, "Payment ID is already linked to a different order.");
        Long localPaymentId = paymentIds.get(0);
        db.update("UPDATE training_payment_orders SET status='PAID',updated_at=CURRENT_TIMESTAMP WHERE id=?", order.get("id"));
        db.update("""
                INSERT INTO payment_transactions_ledger(enrollment_id,payment_id,transaction_type,amount,currency,reference_id,description)
                SELECT ?,?, 'PAYMENT',?,?,?, 'Razorpay enrollment payment captured'
                 WHERE NOT EXISTS (SELECT 1 FROM payment_transactions_ledger WHERE transaction_type='PAYMENT' AND reference_id=?)
                """, order.get("enrollment_id"), localPaymentId, amount, currency, paymentId, paymentId);
        return localPaymentId;
    }

    private void requireConfigured() {
        if (keyId.isBlank() || keySecret.isBlank()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Razorpay is not configured.");
    }
}
