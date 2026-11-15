package com.app.admin.controller;

import com.app.admin.dto.RazorpayOrderRequest;
import com.app.admin.dto.RazorpayPaymentVerificationRequest;
import com.app.admin.service.RazorpayCheckoutService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/payments")
public class RazorpayCheckoutController {
    private final RazorpayCheckoutService checkout;
    public RazorpayCheckoutController(RazorpayCheckoutService checkout) { this.checkout = checkout; }

    @PostMapping("/create-order")
    public Map<String, Object> createOrder(@Valid @RequestBody RazorpayOrderRequest request) {
        return checkout.createOrder(request);
    }

    @PostMapping("/verify-payment")
    public Map<String, Object> verifyPayment(@Valid @RequestBody RazorpayPaymentVerificationRequest request) {
        return checkout.verifyPayment(request);
    }
}