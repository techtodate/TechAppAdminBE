package com.app.admin.service.institution;

import java.util.*;
import org.springframework.stereotype.Service;

import com.app.admin.dto.MappedInstitutionData;
import com.app.admin.dto.ValidationResult;
import com.app.admin.model.Country;
import com.app.admin.model.InstitutionSource;

@Service
public class InstitutionValidationService {

    public ValidationResult validateRow(
            MappedInstitutionData data,
            InstitutionSource source,
            Country expectedCountry,
            Set<String> seenIdentifiersInImport) {
        List<String> errors = new ArrayList<>();

        // 1. Source identifier
        if (data.getSourceIdentifier() == null || data.getSourceIdentifier().isBlank()) {
            errors.add("Source identifier is required and cannot be blank");
        } else {
            String ident = data.getSourceIdentifier().trim();
            if (seenIdentifiersInImport != null && seenIdentifiersInImport.contains(ident)) {
                errors.add("Duplicate source identifier inside the import: " + ident);
            }
        }

        // 2. Institution name
        if (data.getName() == null || data.getName().isBlank()) {
            errors.add("Institution name is required and cannot be blank");
        } else if (data.getName().length() > 255) {
            errors.add("Institution name exceeds maximum length of 255 characters");
        }

        if (!errors.isEmpty()) {
            return ValidationResult.invalid(errors);
        }

        if (seenIdentifiersInImport != null && data.getSourceIdentifier() != null) {
            seenIdentifiersInImport.add(data.getSourceIdentifier().trim());
        }

        return ValidationResult.ok();
    }

}
