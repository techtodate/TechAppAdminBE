package com.app.admin.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import java.util.Map;

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
    private String countryCode;
    private String administrativeAreaCode;
    private String subAdministrativeArea;
    private Long locationId;
    private String city;
    private String address;
    private String website;
    private String normalizedWebsite;
    private String resolvedCityName;
    private Map<String, String> additionalFields;
}
