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
@Table(name = "institution_sources", schema = "public", uniqueConstraints = {
        @UniqueConstraint(name = "uq_institution_source_country_code", columnNames = {"country_id", "source_code"})
})
@Getter @Setter @NoArgsConstructor
public class InstitutionSource {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long id;

    @NotNull
    @Column(name = "country_id", nullable = false)
    private Long countryId;

    @NotBlank
    @Size(max = 100)
    @Column(name = "source_code", nullable = false, length = 100)
    private String sourceCode;

    @NotBlank
    @Size(max = 200)
    @Column(name = "source_name", nullable = false, length = 200)
    private String sourceName;

    @NotBlank
    @Size(max = 50)
    @Column(name = "source_type", nullable = false, length = 50)
    private String sourceType = "GOVERNMENT";

    @Size(max = 500)
    @Column(name = "source_url", length = 500)
    private String sourceUrl;

    @Transient
    private String description;

    @NotBlank
    @Size(max = 50)
    @Column(name = "import_method", nullable = false, length = 50)
    private String importMethod = "FILE_UPLOAD";

    @NotNull
    @Column(nullable = false)
    private Boolean active = true;

    @NotNull
    @Column(nullable = false)
    private Integer priority = 1;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "country_id", insertable = false, updatable = false)
    private Country country;

    public String getCode() {
        return sourceCode;
    }

    public void setCode(String code) {
        this.sourceCode = code;
    }

    public String getName() {
        return sourceName;
    }

    public void setName(String name) {
        this.sourceName = name;
    }

    public String getFileFormat() {
        return "CSV";
    }

    public void setFileFormat(String fileFormat) {
        // backward-compatibility placeholder
    }
}

