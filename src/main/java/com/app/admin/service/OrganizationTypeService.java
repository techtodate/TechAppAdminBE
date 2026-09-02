package com.app.admin.service;
import org.springframework.stereotype.Service;
import com.app.admin.model.OrganizationType;
import com.app.admin.repository.OrganizationTypeRepository;
@Service public class OrganizationTypeService extends MaintenanceMasterService<OrganizationType> {
    public OrganizationTypeService(OrganizationTypeRepository repository) { super(repository, "OrganizationType"); }
}
