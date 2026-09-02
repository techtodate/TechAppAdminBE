package com.app.admin.service;
import org.springframework.stereotype.Service;
import com.app.admin.model.TrainingType;
import com.app.admin.repository.TrainingTypeRepository;
@Service public class TrainingTypeService extends MaintenanceMasterService<TrainingType> {
    public TrainingTypeService(TrainingTypeRepository repository) { super(repository, "TrainingType"); }
}
