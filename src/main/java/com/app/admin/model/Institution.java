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
@Table(name = "institutions", schema = "public", uniqueConstraints = {
        @UniqueConstraint(name = "uq_institutions_global_identifier", columnNames = {"global_identifier"})
})
@Getter @Setter @NoArgsConstructor
public class Institution {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long id;

    @Size(max = 150)
    @Column(name = "global_identifier", length = 150)
    private String globalIdentifier;

    @NotBlank
    @Size(max = 500)
    @Column(nullable = false, length = 500)
    private String name;

    @NotBlank
    @Size(max = 500)
    @Column(name = "normalized_name", nullable = false, length = 500)
    private String normalizedName;

    @Size(max = 250)
    @Column(name = "short_name", length = 250)
    private String shortName;

    @Size(max = 100)
    @Column(name = "institution_type", length = 100)
    private String institutionType;

    @NotNull
    @Column(name = "country_id", nullable = false)
    private Long countryId;

    @Column(name = "administrative_area_id")
    private Long administrativeAreaId;

    @Column(name = "city_id")
    private Long cityId;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Size(max = 1000)
    @Column(length = 1000)
    private String website;

    @NotNull
    @Size(max = 50)
    @Column(name = "verification_status", nullable = false, length = 50)
    private String verificationStatus = "VERIFIED";

    @Column(name = "source_id")
    private Long sourceId;

    @Size(max = 250)
    @Column(name = "source_identifier", length = 250)
    private String sourceIdentifier;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "country_id", insertable = false, updatable = false)
    private Country country;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "administrative_area_id", insertable = false, updatable = false)
    private AdministrativeArea administrativeArea;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "city_id", insertable = false, updatable = false)
    private City city;

    @Column(name = "city_name")
    private String cityName;

    @Column(name = "normalized_website")
    private String normalizedWebsite;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_id", insertable = false, updatable = false)
    private InstitutionSource source;

    public Long getStateId() {
        return administrativeAreaId;
    }

    public void setStateId(Long stateId) {
        this.administrativeAreaId = stateId;
    }

    public String getCityName() {
        return cityName != null ? cityName : city != null ? city.getName() : null;
    }

    public void setCityName(String cityName) {
        this.cityName = cityName;
    }

    public String getNormalizedWebsite() {
        return normalizedWebsite;
    }

    public void setNormalizedWebsite(String normalizedWebsite) {
        this.normalizedWebsite = normalizedWebsite;
    }
}

