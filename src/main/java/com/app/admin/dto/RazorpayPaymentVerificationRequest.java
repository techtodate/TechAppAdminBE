package com.app.admin.dto;

import jakarta.validation.constraints.NotBlank;

public record RazorpayPaymentVerificationRequest(
        @NotBlank String razorpayOrderId,
        @NotBlank String razorpayPaymentId,
        @NotBlank String razorpaySignature) {}