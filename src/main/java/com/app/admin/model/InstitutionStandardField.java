package com.app.admin.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "institution_standard_fields", schema = "public", uniqueConstraints = {
        @UniqueConstraint(name = "uq_institution_standard_field_code", columnNames = {"field_code"})
})
@Getter @Setter @NoArgsConstructor
public class InstitutionStandardField {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long id;

    @NotBlank
    @Size(max = 100)
    @Column(name = "field_code", nullable = false, length = 100)
    private String fieldCode;

    @NotBlank
    @Size(max = 200)
    @Column(name = "field_name", nullable = false, length = 200)
    private String fieldName;

    @NotBlank
    @Size(max = 50)
    @Column(name = "data_type", nullable = false, length = 50)
    private String dataType = "STRING";

    @NotNull
    @Column(name = "is_required", nullable = false)
    private Boolean isRequired = false;

    @Size(max = 500)
    @Column(length = 500)
    private String description;

    @NotNull
    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    public String getCode() {
        return fieldCode;
    }

    public void setCode(String code) {
        this.fieldCode = code;
    }

    public String getName() {
        return fieldName;
    }

    public void setName(String name) {
        this.fieldName = name;
    }
}

