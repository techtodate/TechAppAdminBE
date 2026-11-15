package com.app.admin.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RazorpayRouteLinkedAccountTestRequest(
        @JsonProperty("trainer_user_id") @JsonAlias("trainerUserId") @NotNull Long trainerUserId,
        @JsonProperty("payout_account_id") @JsonAlias("payoutAccountId") @NotNull Long payoutAccountId,
        @JsonProperty("email") @NotBlank @Email @Size(max = 255) String email,
        @JsonProperty("phone") @NotBlank @Pattern(regexp = "[0-9]{10,15}") String phone,
        @JsonProperty("legal_business_name") @JsonAlias("legalBusinessName") @NotBlank @Size(max = 200) String legalBusinessName,
        @JsonProperty("business_type") @JsonAlias("businessType") @NotBlank @Size(max = 100) String businessType,
        @JsonProperty("contact_name") @JsonAlias("contactName") @NotBlank @Size(max = 200) String contactName,
        @JsonProperty("bank_account_name") @JsonAlias("bankAccountName") @NotBlank @Size(max = 100) String bankAccountName,
        @JsonProperty("bank_account_number") @JsonAlias("bankAccountNumber") @NotBlank @Pattern(regexp = "[0-9]{6,30}") String bankAccountNumber,
        @JsonProperty("ifsc") @NotBlank @Pattern(regexp = "[A-Za-z]{4}0[A-Za-z0-9]{6}") String ifsc,
        @JsonProperty("confirmation") @NotBlank String confirmation) {}
