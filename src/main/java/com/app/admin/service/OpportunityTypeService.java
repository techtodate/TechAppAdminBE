package com.app.admin.service;
import org.springframework.stereotype.Service;
import com.app.admin.model.OpportunityType;
import com.app.admin.repository.OpportunityTypeRepository;
@Service public class OpportunityTypeService extends MaintenanceMasterService<OpportunityType> {
    public OpportunityTypeService(OpportunityTypeRepository repository) { super(repository, "OpportunityType"); }
}
