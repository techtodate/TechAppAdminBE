package com.app.admin.service.institution;

import java.util.*;
import org.springframework.stereotype.Service;

import com.app.admin.dto.MappedInstitutionData;
import com.app.admin.dto.SourceRow;
import com.app.admin.exception.InstitutionException;
import com.app.admin.model.InstitutionSourceMapping;
import com.app.admin.model.TransformationRule;
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

    public MappedInstitutionData mapRow(SourceRow row, List<InstitutionSourceMapping> mappings) {
        Map<String, String> values = row.values();

        // Create a case-insensitive lookup map of row values
        Map<String, String> lookup = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        lookup.putAll(values);

        MappedInstitutionData.MappedInstitutionDataBuilder builder = MappedInstitutionData.builder();
        Map<String, String> additionalFields = new LinkedHashMap<>();

        for (InstitutionSourceMapping mapping : mappings) {
            String sourceField = mapping.getSourceFieldName();
            String rawVal = lookup.get(sourceField);
            if ((rawVal == null || rawVal.trim().isEmpty()) && mapping.getDefaultValue() != null) {
                rawVal = mapping.getDefaultValue();
            }

            String transformed = applyTransformation(rawVal, mapping.getTransformationRule() == null || mapping.getTransformationRule().isBlank() ? null : TransformationRule.valueOf(mapping.getTransformationRule().toUpperCase(Locale.ROOT)));
            String code = mapping.getStandardFieldCode();

            switch (code) {
                case "SOURCE_IDENTIFIER" -> builder.sourceIdentifier(transformed);
                case "NAME" -> {
                    String nameFromFirstLetter = stripLeadingNonLetters(rawVal);
                    builder.name(normalizationService.normalizeDisplayName(nameFromFirstLetter));
                    builder.normalizedName(normalizationService.createMatchNormalizedName(nameFromFirstLetter));
                }
                case "SHORT_NAME" -> builder.shortName(transformed);
                case "INSTITUTION_TYPE" -> builder.institutionType(transformed);
                case "COUNTRY" -> { builder.country(transformed); builder.countryCode(transformed); }
                case "COUNTRY_CODE" -> builder.countryCode(transformed);
                case "ADMINISTRATIVE_AREA" -> { builder.administrativeArea(transformed); builder.administrativeAreaCode(transformed); }
                case "ADMINISTRATIVE_AREA_CODE" -> builder.administrativeAreaCode(transformed);
                case "SUB_ADMINISTRATIVE_AREA" -> builder.subAdministrativeArea(transformed);
                case "CITY" -> { builder.city(transformed); builder.resolvedCityName(transformed); }
                case "LOCATION_ID" -> {
                    if (transformed != null && !transformed.isBlank()) {
                        try { builder.locationId(Long.valueOf(transformed)); }
                        catch (NumberFormatException ex) { throw new IllegalArgumentException("LOCATION_ID must be a whole number"); }
                    }
                }
                case "ADDRESS" -> builder.address(transformed);
                case "WEBSITE" -> {
                    builder.website(rawVal);
                    builder.normalizedWebsite(null);
                }
                default -> additionalFields.put(code, transformed);
            }
        }

        // Keep imported values as supplied; do not resolve or persist master IDs.
        builder.additionalFields(additionalFields);

        return builder.build();
    }

    private String stripLeadingNonLetters(String value) {
        if (value == null) return null;
        return value.replaceFirst("^[^\\p{L}]+", "");
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
