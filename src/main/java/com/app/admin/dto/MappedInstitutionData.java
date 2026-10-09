package com.app.admin.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public class MappedInstitutionData {
    private String sourceIdentifier;
    private String name;
    private String normalizedName;
    private String shortName;
    private String institutionType;
    private String country;
    private String administrativeArea;
    private String city;
    private String address;
    private String website;
    private String normalizedWebsite;
    private Long countryId;
    private Long stateId;
    private Long cityId;
    private String resolvedCityName;
}
