package com.app.admin.service.institution;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.app.admin.dto.*;
import com.app.admin.model.*;
import com.app.admin.repository.*;
import com.app.admin.security.AdminSecurityContext;
import com.app.admin.exception.ResourceNotFoundException;
import jakarta.persistence.criteria.Predicate;

/** Builds explicit API values within a transaction; no Hibernate entities escape to Jackson. */
@Service
@Transactional(readOnly = true)
public class InstitutionApiService {
    private final InstitutionRepository institutions;
    private final InstitutionSourceRepository sources;
    private final InstitutionImportRepository imports;
    private final InstitutionImportRecordRepository records;
    private final InstitutionSourceMappingRepository mappings;
    private final InstitutionStandardFieldRepository fields;
    private final InstitutionAliasRepository aliases;
    private final InstitutionSourceRecordRepository sourceRecords;
    private final CountryRepository countries;
    private final InstitutionImportService workflow;
    private final AdminSecurityContext security;

    public InstitutionApiService(InstitutionRepository institutions, InstitutionSourceRepository sources,
            InstitutionImportRepository imports, InstitutionImportRecordRepository records,
            InstitutionSourceMappingRepository mappings, InstitutionStandardFieldRepository fields,
            InstitutionAliasRepository aliases, InstitutionSourceRecordRepository sourceRecords,
            CountryRepository countries, InstitutionImportService workflow, AdminSecurityContext security) {
        this.institutions = institutions; this.sources = sources; this.imports = imports; this.records = records;
        this.mappings = mappings; this.fields = fields; this.aliases = aliases; this.sourceRecords = sourceRecords;
        this.countries = countries; this.workflow = workflow; this.security = security;
    }

    public static Map<String, Object> values(Object... pairs) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) result.put((String) pairs[i], pairs[i + 1]);
        return result;
    }
    public static <T> Map<String, Object> page(Page<T> page, Function<T, Map<String, Object>> mapper) {
        return values("content", page.getContent().stream().map(mapper).toList(), "total_elements", page.getTotalElements(),
                "total_pages", page.getTotalPages(), "number", page.getNumber(), "size", page.getSize());
    }
    private void viewPermission() { security.requirePermission(AdminSecurityContext.PERMISSION_INSTITUTION_VIEW); }
    private Map<String, Object> country(Country value) { return value == null ? null : values("id", value.getId(), "name", value.getName(), "code", value.getIsoCode()); }
    public List<Map<String, Object>> countries() { viewPermission(); return countries.findAll(Sort.by("name", "id")).stream().map(this::country).toList(); }

    public Map<String, Object> source(InstitutionSource value) {
        if (value == null) return null;
        return values("id", value.getId(), "country_id", value.getCountryId(), "country", country(value.getCountry()),
                "source_name", value.getSourceName(), "source_code", value.getSourceCode(), "source_type", value.getSourceType(),
                "source_url", value.getSourceUrl(), "description", value.getDescription(), "import_method", value.getImportMethod(),
                "active", value.getActive(), "priority", value.getPriority());
    }
    public List<Map<String, Object>> sources(Long countryId) {
        viewPermission();
        List<InstitutionSource> result = countryId == null ? sources.findAll(Sort.by("sourceName")) : sources.findByCountryId(countryId, Sort.by("sourceName"));
        return result.stream().map(this::source).toList();
    }
    public Map<String, Object> institution(Institution value) {
        return values("id", value.getId(), "name", value.getName(), "short_name", value.getShortName(),
                "institution_type", value.getInstitutionType(), "country_id", value.getCountryId(), "country", country(value.getCountry()),
                "administrative_area_id", value.getAdministrativeAreaId(), "administrative_area", value.getAdministrativeArea() == null ? null : values("id", value.getAdministrativeArea().getId(), "name", value.getAdministrativeArea().getName()),
                "city_id", value.getCityId(), "city", value.getCity() == null ? null : values("id", value.getCity().getId(), "name", value.getCity().getName()),
                "address", value.getAddress(), "website", value.getWebsite(), "verification_status", value.getVerificationStatus(),
                "active", value.getActive(), "source", source(value.getSource()), "source_identifier", value.getSourceIdentifier(),
                "created_at", value.getCreatedAt(), "updated_at", value.getUpdatedAt());
    }
    public Map<String, Object> institutionDetails(Long id) {
        viewPermission();
        Map<String, Object> result = institution(institutions.findById(id).orElseThrow(() -> new ResourceNotFoundException("Institution", id)));
        result.put("aliases", aliases.findByInstitutionId(id).stream().map(a -> values("id", a.getId(), "alias_name", a.getAliasName())).toList());
        List<InstitutionSourceRecord> linked = sourceRecords.findByInstitutionId(id);
        result.put("source_records", linked.stream().map(r -> values("id", r.getId(), "source", source(r.getSource()), "source_identifier", r.getSourceIdentifier(), "last_seen_at", r.getLastSeenAt(), "raw_data", r.getSourceData())).toList());
        result.put("last_seen_at", linked.stream().map(InstitutionSourceRecord::getLastSeenAt).filter(Objects::nonNull).max(Comparator.naturalOrder()).orElse(null));
        return result;
    }
    public Map<String, Object> list(Long countryId, String q, String type, String verification, Boolean active, Pageable pageable) {
        viewPermission();
        Specification<Institution> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (countryId != null) predicates.add(cb.equal(root.get("countryId"), countryId));
            if (type != null && !type.isBlank()) predicates.add(cb.equal(root.get("institutionType"), type));
            if (verification != null && !verification.isBlank()) predicates.add(cb.equal(root.get("verificationStatus"), verification));
            if (active != null) predicates.add(cb.equal(root.get("active"), active));
            if (q != null && !q.isBlank()) { String pattern = "%" + q.trim().toLowerCase(Locale.ROOT).replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
                predicates.add(cb.or(cb.like(cb.lower(root.get("name")), pattern, '\\'), cb.like(cb.lower(root.get("shortName")), pattern, '\\'))); }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        return page(institutions.findAll(spec, pageable), this::institution);
    }
    public Map<String, Object> metadata() {
        viewPermission(); return values("institution_types", institutions.findInstitutionTypes(), "verification_statuses", List.of("VERIFIED", "PENDING", "UNVERIFIED"));
    }
    public Map<String, Object> dashboard() {
        viewPermission(); List<InstitutionSource> all = sources.findAll();
        Page<InstitutionImport> recent = imports.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 5));
        return values("total_institutions", institutions.count(), "configured_sources", all.size(),
                "countries_with_sources", all.stream().map(InstitutionSource::getCountryId).distinct().count(),
                "total_imports", imports.count(), "recent_imports", recent.getContent().stream().map(this::importView).toList());
    }
    public List<Map<String, Object>> fields() {
        viewPermission(); return fields.findAll(Sort.by("displayOrder", "id")).stream().map(f -> values("id", f.getId(), "field_code", f.getFieldCode(), "field_name", f.getFieldName(), "is_required", f.getIsRequired(), "active", f.getActive())).toList();
    }
    public List<Map<String, Object>> mappings(Long sourceId) {
        viewPermission(); return (sourceId == null ? mappings.findAll() : mappings.findBySourceId(sourceId)).stream().map(this::mapping).toList();
    }
    private Map<String, Object> mapping(InstitutionSourceMapping m) {
        return values("id", m.getId(), "source_id", m.getSourceId(), "source_field_name", m.getSourceFieldName(), "standard_field_code", m.getStandardFieldCode(), "transformation_rule", m.getTransformationRule(), "is_required", m.getIsRequired(), "active", m.getActive());
    }
    @Transactional
    public List<Map<String, Object>> saveMappings(Long sourceId, List<SaveInstitutionMapping> request) {
        security.requirePermission(AdminSecurityContext.PERMISSION_INSTITUTION_MANAGE);
        if (!sources.existsById(sourceId)) throw new ResourceNotFoundException("Institution source", sourceId);
        Map<String, InstitutionStandardField> valid = new HashMap<>(); fields.findAll().stream().filter(f -> Boolean.TRUE.equals(f.getActive())).forEach(f -> valid.put(f.getFieldCode(), f));
        Set<String> names = new HashSet<>(), destinations = new HashSet<>();
        for (SaveInstitutionMapping row : request) {
            if (!names.add(row.sourceFieldName().trim().toLowerCase(Locale.ROOT))) throw new IllegalArgumentException("Source field names must be unique");
            if (!valid.containsKey(row.standardFieldCode()) || !destinations.add(row.standardFieldCode())) throw new IllegalArgumentException("Destination fields must be valid and unique");
            StandardFieldCode.valueOf(row.standardFieldCode());
            if (row.transformationRule() != null && !row.transformationRule().isBlank()) TransformationRule.valueOf(row.transformationRule());
        }
        if (!destinations.containsAll(List.of("SOURCE_IDENTIFIER", "NAME")) || valid.values().stream().anyMatch(f -> Boolean.TRUE.equals(f.getIsRequired()) && !destinations.contains(f.getFieldCode()))) throw new IllegalArgumentException("Source identifier, name and all required fields must be mapped");
        List<InstitutionSourceMapping> old = mappings.findBySourceId(sourceId);
        // Update by source-field key so existing mappings retain transformation configuration.
        Map<String, InstitutionSourceMapping> byName = new HashMap<>(); old.forEach(m -> byName.put(m.getSourceFieldName(), m));
        List<InstitutionSourceMapping> saved = new ArrayList<>();
        for (SaveInstitutionMapping row : request) {
            InstitutionSourceMapping m = byName.remove(row.sourceFieldName().trim());
            if (m == null) m = new InstitutionSourceMapping();
            m.setSourceId(sourceId); m.setSourceFieldName(row.sourceFieldName().trim()); m.setStandardFieldCode(row.standardFieldCode());
            m.setTransformationRule(row.transformationRule()); m.setIsRequired(Boolean.TRUE.equals(valid.get(row.standardFieldCode()).getIsRequired())); m.setActive(true); saved.add(m);
        }
        mappings.deleteAll(byName.values()); mappings.flush();
        return mappings.saveAll(saved).stream().map(this::mapping).toList();
    }
    public Map<String, Object> importView(InstitutionImport value) {
        return values("id", value.getId(), "country_id", value.getCountryId(), "country", countries.findById(value.getCountryId()).map(this::country).orElse(null),
                "source_id", value.getSourceId(), "source", sources.findById(value.getSourceId()).map(this::source).orElse(null),
                "version", value.getVersion(), "import_type", value.getImportType(), "file_name", value.getFileName(), "status", value.getStatus(),
                "started_at", value.getStartedAt(), "completed_at", value.getCompletedAt(), "created_at", value.getCreatedAt(),
                "total_records", value.getTotalRecords(), "valid_records", value.getValidRecords(), "new_count", value.getNewCount(),
                "inserted_count", value.getStatus() == ImportStatus.COMPLETED ? value.getInsertedCount() : 0,
                "updated_count", value.getUpdatedCount(), "unchanged_count", value.getUnchangedCount(), "duplicate_count", value.getDuplicateCount(), "error_count", value.getErrorCount());
    }
    public Map<String, Object> getImport(Long id) { viewPermission(); return importView(workflow.getImport(id)); }
    public Map<String, Object> preview(Long id) { viewPermission(); return preview(workflow.previewImport(id)); }
    private Map<String, Object> preview(ImportPreviewResponse value) {
        ImportSummaryDto s = value.summary(); return values("import_id", value.importId(), "status", value.status(), "summary", values("total_records", s.totalRecords(), "valid_records", s.validRecords(), "new_count", s.newCount(), "updated_count", s.updatedCount(), "unchanged_count", s.unchangedCount(), "duplicate_count", s.duplicateCount(), "error_count", s.errorCount()));
    }
    public Map<String, Object> record(InstitutionImportRecord r) {
        return values("id", r.getId(), "row_number", r.getRowNumber(), "source_identifier", r.getSourceIdentifier(), "raw_data", r.getRawData(),
                "normalized_data", r.getNormalizedData(), "action", r.getAction(), "processing_status", r.getProcessingStatus(), "validation_status", r.getValidationStatus(),
                "institution_id", r.getInstitutionId(), "institution", r.getInstitutionId() == null ? null : institutions.findById(r.getInstitutionId()).map(this::institution).orElse(null), "match_confidence", r.getMatchConfidence(), "error_message", r.getErrorMessage());
    }
    public Map<String, Object> record(Long importId, Long recordId) { viewPermission(); return record(workflow.getImportRecord(importId, recordId)); }
    public Map<String, Object> records(Long importId, String classification, String q, Pageable pageable) {
        viewPermission(); workflow.getImport(importId);
        if (classification != null && !classification.isBlank()) RecordClassification.valueOf(classification);
        Specification<InstitutionImportRecord> spec = (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>(); p.add(cb.equal(root.get("importId"), importId));
            if (classification != null && !classification.isBlank()) p.add(cb.equal(root.get("action"), classification));
            if (q != null && !q.isBlank()) { String pattern = "%" + q.trim().toLowerCase(Locale.ROOT).replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
                p.add(cb.or(cb.like(cb.lower(root.get("sourceIdentifier")), pattern, '\\'), cb.like(cb.lower(cb.function("jsonb_extract_path_text", String.class, root.get("normalizedData"), cb.literal("name"))), pattern, '\\'))); }
            return cb.and(p.toArray(Predicate[]::new));
        };
        return page(records.findAll(spec, pageable), this::record);
    }
    private Map<String, Object> match(InstitutionImportRecordMatch m) { return values("id", m.getId(), "import_record_id", m.getImportRecordId(), "institution_id", m.getInstitutionId(), "match_score", m.getMatchScore(), "match_reason", m.getMatchReason(), "resolution", m.getResolution(), "institution", institution(m.getInstitution()), "import_record", record(m.getImportRecord())); }
    public List<Map<String, Object>> duplicates(Long id) { viewPermission(); workflow.getImport(id); return workflow.getImportDuplicates(id).stream().map(this::match).toList(); }
    public Map<String, Object> history(Long countryId, Long sourceId, String status, LocalDate from, LocalDate to, Pageable pageable) {
        viewPermission(); if (from != null && to != null && from.isAfter(to)) throw new IllegalArgumentException("dateFrom must be before dateTo");
        ImportStatus parsed = status == null || status.isBlank() ? null : ImportStatus.valueOf(status);
        Specification<InstitutionImport> spec = (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (countryId != null) p.add(cb.equal(root.get("countryId"), countryId)); if (sourceId != null) p.add(cb.equal(root.get("sourceId"), sourceId));
            if (parsed != null) p.add(cb.equal(root.get("status"), parsed));
            if (from != null) p.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from.atStartOfDay()));
            if (to != null) p.add(cb.lessThan(root.get("createdAt"), to.plusDays(1).atStartOfDay()));
            return cb.and(p.toArray(Predicate[]::new));
        };
        return page(imports.findAll(spec, pageable), this::importView);
    }
    @Transactional public Map<String, Object> create(CreateInstitutionImportRequest request) { return importView(workflow.createImport(request)); }
    @Transactional public Map<String, Object> upload(Long id, org.springframework.web.multipart.MultipartFile file) { return importView(workflow.uploadSourceFile(id, file)); }
    @Transactional public Map<String, Object> validate(Long id) { return preview(workflow.validateImport(id)); }
    @Transactional public Map<String, Object> apply(Long id) { return importView(workflow.applyImport(id)); }
    @Transactional public Map<String, Object> cancel(Long id) { return importView(workflow.cancelImport(id)); }
    @Transactional public Map<String, Object> resolve(Long id, Long matchId, boolean merge) { return match(merge ? workflow.mergeDuplicate(id, matchId) : workflow.keepDuplicateSeparate(id, matchId)); }
}
