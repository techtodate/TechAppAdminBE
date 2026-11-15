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

@Entity @Table(name = "refund_policy_rules", schema = "public") @Getter @Setter @NoArgsConstructor
public class RefundPolicyRule {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @JsonProperty(access = JsonProperty.Access.READ_ONLY) private Long id;
    @NotBlank @Size(max = 80) @Column(nullable = false, unique = true, length = 80) private String code;
    @NotBlank @Size(max = 150) @Column(nullable = false, length = 150) private String name;
    @Size(max = 1000) @Column(length = 1000) private String description;
    @NotNull @DecimalMin("0.00") @DecimalMax("100.00") @Column(name = "refund_percentage", nullable = false, precision = 5, scale = 2) private BigDecimal refundPercentage;
    @NotNull @Column(nullable = false) private Boolean active = true;
    @NotNull @Min(0) @Column(name = "display_order", nullable = false) private Integer displayOrder = 0;
    @CreationTimestamp @Column(name = "created_at", updatable = false) @JsonProperty(access = JsonProperty.Access.READ_ONLY) private LocalDateTime createdAt;
    @UpdateTimestamp @Column(name = "updated_at") @JsonProperty(access = JsonProperty.Access.READ_ONLY) private LocalDateTime updatedAt;
}
