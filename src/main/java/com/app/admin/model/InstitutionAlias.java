package com.app.admin.model;

import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "institution_aliases", schema = "public")
@Getter @Setter @NoArgsConstructor
public class InstitutionAlias {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long id;

    @NotNull
    @Column(name = "institution_id", nullable = false)
    private Long institutionId;

    @NotBlank
    @Size(max = 500)
    @Column(name = "alias_name", nullable = false, length = 500)
    private String aliasName;

    @NotBlank
    @Size(max = 500)
    @Column(name = "normalized_alias_name", nullable = false, length = 500)
    private String normalizedAliasName;

    @Size(max = 50)
    @Column(name = "alias_type", length = 50)
    private String aliasType;

    @Column(name = "source_id")
    private Long sourceId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime createdAt;

    @org.hibernate.annotations.UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "institution_id", insertable = false, updatable = false)
    private Institution institution;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_id", insertable = false, updatable = false)
    private InstitutionSource source;

    public String getNormalizedAlias() {
        return normalizedAliasName;
    }

    public void setNormalizedAlias(String normalizedAlias) {
        this.normalizedAliasName = normalizedAlias;
    }
}
