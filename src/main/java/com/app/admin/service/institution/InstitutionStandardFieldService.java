package com.app.admin.service.institution;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.admin.dto.InstitutionStandardFieldRequest;
import com.app.admin.exception.ResourceNotFoundException;
import com.app.admin.model.InstitutionStandardField;
import com.app.admin.repository.InstitutionStandardFieldRepository;
import com.app.admin.security.AdminSecurityContext;

@Service
@Transactional(readOnly = true)
public class InstitutionStandardFieldService {
    private final InstitutionStandardFieldRepository fields;
    private final AdminSecurityContext security;

    public InstitutionStandardFieldService(InstitutionStandardFieldRepository fields, AdminSecurityContext security) {
        this.fields = fields;
        this.security = security;
    }

    public List<Map<String, Object>> list() {
        requireViewPermission();
        return fields.findAll(Sort.by(Sort.Order.asc("displayOrder"), Sort.Order.asc("id")))
                .stream().map(this::view).toList();
    }

    public Map<String, Object> get(Long id) {
        requireViewPermission();
        return view(find(id));
    }

    @Transactional
    public Map<String, Object> create(InstitutionStandardFieldRequest request) {
        requireManagePermission();
        ensureUniqueCode(request.field_code(), null);
        InstitutionStandardField field = new InstitutionStandardField();
        apply(field, request);
        return view(fields.saveAndFlush(field));
    }

    @Transactional
    public Map<String, Object> update(Long id, InstitutionStandardFieldRequest request) {
        requireManagePermission();
        InstitutionStandardField field = find(id);
        ensureUniqueCode(request.field_code(), id);
        apply(field, request);
        return view(fields.saveAndFlush(field));
    }

    @Transactional
    public void delete(Long id) {
        requireManagePermission();
        fields.delete(find(id));
        fields.flush();
    }

    private InstitutionStandardField find(Long id) {
        return fields.findById(id).orElseThrow(() -> new ResourceNotFoundException("Institution standard field", id));
    }

    private void ensureUniqueCode(String code, Long id) {
        String normalized = code.trim();
        boolean duplicate = fields.findAll().stream().anyMatch(field -> !field.getId().equals(id)
                && field.getFieldCode().toLowerCase(Locale.ROOT).equals(normalized.toLowerCase(Locale.ROOT)));
        if (duplicate) throw new IllegalArgumentException("field_code already exists");
    }

    private void apply(InstitutionStandardField field, InstitutionStandardFieldRequest request) {
        field.setFieldCode(request.field_code().trim());
        field.setFieldName(request.field_name().trim());
        field.setDataType(request.data_type().trim());
        field.setIsRequired(request.required());
        field.setDescription(request.description() == null || request.description().isBlank() ? null : request.description().trim());
        field.setActive(request.active());
        field.setDisplayOrder(request.display_order());
    }

    private Map<String, Object> view(InstitutionStandardField field) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", field.getId());
        result.put("field_code", field.getFieldCode());
        result.put("field_name", field.getFieldName());
        result.put("data_type", field.getDataType());
        result.put("required", field.getIsRequired());
        result.put("description", field.getDescription());
        result.put("active", field.getActive());
        result.put("display_order", field.getDisplayOrder());
        return result;
    }

    private void requireViewPermission() {
        security.requirePermission(AdminSecurityContext.PERMISSION_INSTITUTION_VIEW);
    }

    private void requireManagePermission() {
        security.requirePermission(AdminSecurityContext.PERMISSION_INSTITUTION_MANAGE);
    }
}
