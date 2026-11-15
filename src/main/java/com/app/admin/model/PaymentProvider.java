package com.app.admin.model;

import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity @Table(name = "payment_providers", schema = "public") @Getter @Setter @NoArgsConstructor
public class PaymentProvider {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @JsonProperty(access = JsonProperty.Access.READ_ONLY) private Long id;
    @NotBlank @Size(max = 50) @Column(nullable = false, unique = true, length = 50) private String code;
    @NotBlank @Size(max = 100) @Column(nullable = false, length = 100) private String name;
    @NotNull @Column(nullable = false) private Boolean active = true;
    @CreationTimestamp @Column(name = "created_at", updatable = false) @JsonProperty(access = JsonProperty.Access.READ_ONLY) private LocalDateTime createdAt;
    @UpdateTimestamp @Column(name = "updated_at") @JsonProperty(access = JsonProperty.Access.READ_ONLY) private LocalDateTime updatedAt;
}
