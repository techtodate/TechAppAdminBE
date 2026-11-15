package com.app.admin.controller;

import com.app.admin.dto.RazorpayTestOrderRequest;
import com.app.admin.dto.RazorpayTestPaymentVerificationRequest;
import com.app.admin.service.RazorpayTestCheckoutService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/payments/test")
public class RazorpayTestCheckoutController {
    private final RazorpayTestCheckoutService checkout;
    public RazorpayTestCheckoutController(RazorpayTestCheckoutService checkout) { this.checkout = checkout; }

    @PostMapping("/create-order")
    public Map<String, Object> createOrder(@Valid @RequestBody RazorpayTestOrderRequest request) {
        return checkout.createOrder(request);
    }

    @PostMapping("/verify-payment")
    public Map<String, Object> verifyPayment(@Valid @RequestBody RazorpayTestPaymentVerificationRequest request) {
        return checkout.verifyPayment(request);
    }
}
