package com.app.admin.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity @Table(name = "platform_commission_rules", schema = "public") @Getter @Setter @NoArgsConstructor
public class PlatformCommissionRule {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @JsonProperty(access = JsonProperty.Access.READ_ONLY) private Long id;
    @NotBlank @Size(max = 80) @Column(nullable = false, unique = true, length = 80) private String code;
    @NotBlank @Size(max = 150) @Column(nullable = false, length = 150) private String name;
    @Size(max = 1000) @Column(length = 1000) private String description;
    @NotBlank @Size(max = 30) @Column(name = "commission_type", nullable = false, length = 30) private String commissionType;
    @DecimalMin("0.0000") @DecimalMax("100.0000") @Column(precision = 7, scale = 4) private BigDecimal percentage;
    @DecimalMin("0.00") @Column(name = "fixed_amount", precision = 12, scale = 2) private BigDecimal fixedAmount;
    @Column(length = 10) private String currency = "INR";
    @NotNull @Column(nullable = false) private Boolean active = true;
    @CreationTimestamp @Column(name = "created_at", updatable = false) @JsonProperty(access = JsonProperty.Access.READ_ONLY) private LocalDateTime createdAt;
    @UpdateTimestamp @Column(name = "updated_at") @JsonProperty(access = JsonProperty.Access.READ_ONLY) private LocalDateTime updatedAt;
}
