package com.app.admin.model;

import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "countries", schema = "public", uniqueConstraints = {
        @UniqueConstraint(name = "uq_country_iso_code", columnNames = {"iso_code"}),
        @UniqueConstraint(name = "uq_country_name", columnNames = {"name"})
})
@Getter @Setter @NoArgsConstructor
public class Country {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long id;

    @NotBlank
    @Size(max = 10)
    @Column(name = "iso_code", nullable = false, length = 10)
    private String isoCode;

    @Column(name = "iso3_code", length = 10)
    private String iso3Code;

    @NotBlank
    @Size(max = 150)
    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "phone_code", length = 20)
    private String phoneCode;

    @NotNull
    @Column(nullable = false)
    private Boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime updatedAt;

    public String getCode() {
        return iso3Code != null ? iso3Code : isoCode;
    }

    public void setCode(String code) {
        this.isoCode = code;
    }
}
