package com.app.admin.service.institution;

import java.util.*;
import org.springframework.stereotype.Service;

import com.app.admin.dto.MappedInstitutionData;
import com.app.admin.dto.SourceRow;
import com.app.admin.exception.InstitutionException;
import com.app.admin.model.*;
import com.app.admin.repository.InstitutionSourceMappingRepository;

@Service
public class InstitutionMappingService {

    private final InstitutionSourceMappingRepository mappingRepository;
    private final InstitutionNormalizationService normalizationService;

    public InstitutionMappingService(
            InstitutionSourceMappingRepository mappingRepository,
            InstitutionNormalizationService normalizationService) {
        this.mappingRepository = mappingRepository;
        this.normalizationService = normalizationService;
    }

    public List<InstitutionSourceMapping> getMappingsForSource(Long sourceId) {
        List<InstitutionSourceMapping> mappings = mappingRepository.findBySourceId(sourceId).stream().filter(m -> Boolean.TRUE.equals(m.getActive())).toList();
        if (mappings.isEmpty()) {
            throw InstitutionException.mappingNotFound(sourceId);
        }
        for (String required : List.of("SOURCE_IDENTIFIER", "NAME")) {
            if (mappings.stream().noneMatch(m -> required.equals(m.getStandardFieldCode()))) throw InstitutionException.requiredFieldMissing(required);
        }
        return mappings;
    }

    public MappedInstitutionData mapRow(
            SourceRow row,
            List<InstitutionSourceMapping> mappings,
            Country defaultCountry) {
        Map<String, String> values = row.values();

        // Create a case-insensitive lookup map of row values
        Map<String, String> lookup = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        lookup.putAll(values);

        MappedInstitutionData.MappedInstitutionDataBuilder builder = MappedInstitutionData.builder();
        String countryVal = null;
        String adminAreaVal = null;
        String cityVal = null;

        for (InstitutionSourceMapping mapping : mappings) {
            String sourceField = mapping.getSourceFieldName();
            String rawVal = lookup.get(sourceField);
            if ((rawVal == null || rawVal.trim().isEmpty()) && mapping.getDefaultValue() != null) {
                rawVal = mapping.getDefaultValue();
            }

            String transformed = applyTransformation(rawVal, mapping.getTransformationRule() == null || mapping.getTransformationRule().isBlank() ? null : TransformationRule.valueOf(mapping.getTransformationRule().toUpperCase(Locale.ROOT)));
            StandardFieldCode code = StandardFieldCode.valueOf(mapping.getStandardFieldCode());

            switch (code) {
                case SOURCE_IDENTIFIER -> builder.sourceIdentifier(transformed);
                case NAME -> {
                    builder.name(normalizationService.normalizeDisplayName(rawVal));
                    builder.normalizedName(normalizationService.createMatchNormalizedName(rawVal));
                }
                case SHORT_NAME -> builder.shortName(transformed);
                case INSTITUTION_TYPE -> builder.institutionType(transformed);
                case COUNTRY -> countryVal = transformed;
                case ADMINISTRATIVE_AREA -> adminAreaVal = transformed;
                case CITY -> cityVal = transformed;
                case ADDRESS -> builder.address(transformed);
                case WEBSITE -> {
                    builder.website(normalizationService.normalizeWebsite(rawVal));
                    builder.normalizedWebsite(normalizationService.createNormalizedWebsite(rawVal));
                }
            }
        }

        // Location resolution
        Long countryId = defaultCountry != null ? defaultCountry.getId() : null;
        if (countryVal != null && !countryVal.isBlank()) {
            Optional<Country> resolvedCountry = normalizationService.resolveCountry(countryVal);
            if (resolvedCountry.isPresent()) {
                countryId = resolvedCountry.get().getId();
                builder.country(resolvedCountry.get().getName());
            } else {
                builder.country(countryVal);
            }
        } else if (defaultCountry != null) {
            builder.country(defaultCountry.getName());
        }
        builder.countryId(countryId);

        Long stateId = null;
        if (countryId != null && adminAreaVal != null && !adminAreaVal.isBlank()) {
            Optional<AdministrativeArea> resolvedState = normalizationService.resolveState(countryId, adminAreaVal);
            if (resolvedState.isPresent()) {
                stateId = resolvedState.get().getId();
                builder.administrativeArea(resolvedState.get().getName());
            } else {
                builder.administrativeArea(adminAreaVal);
            }
        }
        builder.stateId(stateId);

        Long cityId = null;
        if (cityVal != null && !cityVal.isBlank()) {
            Optional<City> resolvedCity = normalizationService.resolveCity(countryId, stateId, cityVal);
            if (resolvedCity.isPresent()) {
                cityId = resolvedCity.get().getId();
                builder.resolvedCityName(resolvedCity.get().getName());
                builder.city(resolvedCity.get().getName());
            } else {
                builder.city(cityVal);
                builder.resolvedCityName(cityVal);
            }
        }
        builder.cityId(cityId);

        return builder.build();
    }

    public String applyTransformation(String value, TransformationRule rule) {
        if (value == null) return null;
        String trimmed = value.trim();
        if (rule == null) return trimmed;

        return switch (rule) {
            case TRIM -> trimmed;
            case NORMALIZE_NAME -> normalizationService.normalizeDisplayName(trimmed);
            case NORMALIZE_TYPE -> trimmed.toUpperCase(Locale.ROOT);
            case NORMALIZE_LOCATION -> normalizationService.normalizeLocation(trimmed);
            case NORMALIZE_URL -> normalizationService.normalizeWebsite(trimmed);
        };
    }
}
