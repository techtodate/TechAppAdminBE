package com.app.admin.dto;

import jakarta.validation.constraints.NotBlank;

public record RazorpayTestPaymentVerificationRequest(
        @NotBlank String razorpayOrderId,
        @NotBlank String razorpayPaymentId,
        @NotBlank String razorpaySignature) {}
