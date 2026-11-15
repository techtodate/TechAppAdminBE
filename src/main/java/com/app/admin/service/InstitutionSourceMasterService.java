package com.app.admin.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.admin.exception.ResourceNotFoundException;
import com.app.admin.model.Country;
import com.app.admin.model.InstitutionImport;
import com.app.admin.model.InstitutionSource;
import com.app.admin.repository.CountryRepository;
import com.app.admin.repository.InstitutionImportRepository;
import com.app.admin.repository.InstitutionSourceRepository;
import com.app.admin.security.AdminSecurityContext;

@Service
@Transactional(readOnly = true)
public class InstitutionSourceMasterService {
    private final InstitutionSourceRepository sources;
    private final CountryRepository countries;
    private final InstitutionImportRepository imports;
    private final AdminSecurityContext security;

    public InstitutionSourceMasterService(InstitutionSourceRepository sources, CountryRepository countries,
            InstitutionImportRepository imports, AdminSecurityContext security) {
        this.sources = sources;
        this.countries = countries;
        this.imports = imports;
        this.security = security;
    }

    public List<Map<String, Object>> list(Long countryId, Boolean active, String query) {
        viewPermission();
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        return sources.findAll(Sort.by(Sort.Order.asc("priority"), Sort.Order.asc("sourceName"), Sort.Order.asc("id")))
                .stream().filter(s -> countryId == null || countryId.equals(s.getCountryId()))
                .filter(s -> active == null || active.equals(s.getActive()))
                .filter(s -> q.isEmpty() || contains(s.getSourceCode(), q) || contains(s.getSourceName(), q)
                        || contains(s.getSourceType(), q) || (s.getCountry() != null && contains(s.getCountry().getName(), q)))
                .map(this::view).toList();
    }

    public Map<String, Object> get(Long id) {
        viewPermission();
        return view(find(id));
    }

    @Transactional
    public Map<String, Object> create(InstitutionSource request) {
        managePermission();
        validateCountry(request.getCountryId());
        normalize(request);
        ensureUnique(request, null);
        request.setId(null);
        request.setCreatedAt(null);
        request.setUpdatedAt(null);
        return view(sources.saveAndFlush(request));
    }

    @Transactional
    public Map<String, Object> update(Long id, InstitutionSource request) {
        managePermission();
        InstitutionSource current = find(id);
        validateCountry(request.getCountryId());
        normalize(request);
        ensureUnique(request, id);
        current.setCountryId(request.getCountryId());
        current.setSourceCode(request.getSourceCode());
        current.setSourceName(request.getSourceName());
        current.setSourceType(request.getSourceType());
        current.setSourceUrl(request.getSourceUrl());
        current.setDescription(request.getDescription());
        current.setImportMethod(request.getImportMethod());
        current.setActive(request.getActive());
        current.setPriority(request.getPriority());
        return view(sources.saveAndFlush(current));
    }

    @Transactional
    public void delete(Long id) {
        managePermission();
        sources.delete(find(id));
        sources.flush();
    }

    public List<Map<String, Object>> countries() {
        viewPermission();
        return countries.findAll(Sort.by("name", "id")).stream().map(c -> values("id", c.getId(), "name", c.getName(), "iso_code", c.getIsoCode(), "active", c.getActive())).toList();
    }

    private InstitutionSource find(Long id) {
        return sources.findById(id).orElseThrow(() -> new ResourceNotFoundException("Institution source", id));
    }

    private void validateCountry(Long countryId) {
        if (countryId == null || !countries.existsById(countryId)) throw new ResourceNotFoundException("Country", countryId);
    }

    private void normalize(InstitutionSource s) {
        s.setSourceCode(s.getSourceCode().trim());
        s.setSourceName(s.getSourceName().trim());
        s.setSourceType(s.getSourceType().trim());
        s.setImportMethod(s.getImportMethod() == null || s.getImportMethod().isBlank() ? "FILE_UPLOAD" : s.getImportMethod().trim());
        if (s.getSourceUrl() != null) s.setSourceUrl(s.getSourceUrl().trim());
        if (s.getDescription() != null) s.setDescription(s.getDescription().trim());
        if (s.getActive() == null) s.setActive(true);
        if (s.getPriority() == null) s.setPriority(1);
        if (s.getPriority() < 1) throw new IllegalArgumentException("priority must be greater than or equal to 1");
    }

    private void ensureUnique(InstitutionSource s, Long id) {
        boolean exists = id == null ? sources.existsByCountryIdAndSourceCodeIgnoreCase(s.getCountryId(), s.getSourceCode())
                : sources.existsByCountryIdAndSourceCodeIgnoreCaseAndIdNot(s.getCountryId(), s.getSourceCode(), id);
        if (exists) throw new IllegalArgumentException("source_code already exists for this country");
    }

    private Map<String, Object> view(InstitutionSource s) {
        Country c = s.getCountry();
        InstitutionImport last = imports.findFirstBySourceIdOrderByCreatedAtDesc(s.getId()).orElse(null);
        return values("id", s.getId(), "country_id", s.getCountryId(), "country", c == null ? null : values("id", c.getId(), "name", c.getName(), "iso_code", c.getIsoCode()),
                "source_code", s.getSourceCode(), "source_name", s.getSourceName(), "source_type", s.getSourceType(), "source_url", s.getSourceUrl(),
                "description", s.getDescription(), "import_method", s.getImportMethod(), "active", s.getActive(), "priority", s.getPriority(),
                "status", Boolean.TRUE.equals(s.getActive()) ? "ACTIVE" : "INACTIVE", "created_at", s.getCreatedAt(), "updated_at", s.getUpdatedAt(),
                "last_import", last == null ? null : values("id", last.getId(), "status", last.getStatus(), "created_at", last.getCreatedAt(), "applied_at", last.getAppliedAt()));
    }

    private boolean contains(String value, String q) { return value != null && value.toLowerCase(Locale.ROOT).contains(q); }
    private void viewPermission() { security.requirePermission(AdminSecurityContext.PERMISSION_INSTITUTION_VIEW); }
    private void managePermission() { security.requirePermission(AdminSecurityContext.PERMISSION_INSTITUTION_MANAGE); }
    private Map<String, Object> values(Object... pairs) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) result.put((String) pairs[i], pairs[i + 1]);
        return result;
    }
}
