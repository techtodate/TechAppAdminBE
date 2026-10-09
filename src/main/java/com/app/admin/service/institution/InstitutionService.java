package com.app.admin.service.institution;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.app.admin.exception.ResourceNotFoundException;
import com.app.admin.model.Institution;
import com.app.admin.repository.InstitutionRepository;
import com.app.admin.security.AdminSecurityContext;

@Service
public class InstitutionService {

    private final InstitutionRepository institutionRepository;
    private final AdminSecurityContext securityContext;

    public InstitutionService(InstitutionRepository institutionRepository, AdminSecurityContext securityContext) {
        this.institutionRepository = institutionRepository;
        this.securityContext = securityContext;
    }

    public Page<Institution> listInstitutions(Long countryId, Pageable pageable) {
        securityContext.requirePermission(AdminSecurityContext.PERMISSION_INSTITUTION_VIEW);
        if (countryId != null) {
            return institutionRepository.findByCountryId(countryId, pageable);
        }
        return institutionRepository.findAll(pageable);
    }

    public Institution getInstitution(Long id) {
        securityContext.requirePermission(AdminSecurityContext.PERMISSION_INSTITUTION_VIEW);
        return institutionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Institution not found with id: " + id));
    }

    public Page<Institution> searchInstitutions(Long countryId, String query, Pageable pageable) {
        securityContext.requirePermission(AdminSecurityContext.PERMISSION_INSTITUTION_VIEW);
        String cleanQuery = (query != null) ? query.trim() : "";
        if (countryId != null) {
            return institutionRepository.searchByCountryAndNameOrShortName(countryId, cleanQuery, pageable);
        }
        return institutionRepository.searchByNameOrShortName(cleanQuery, pageable);
    }
}
