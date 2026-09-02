package com.app.admin.service;
import org.springframework.stereotype.Service;
import com.app.admin.model.TrainingDeliveryMode;
import com.app.admin.repository.TrainingDeliveryModeRepository;
@Service public class TrainingDeliveryModeService extends MaintenanceMasterService<TrainingDeliveryMode> {
    public TrainingDeliveryModeService(TrainingDeliveryModeRepository repository) { super(repository, "TrainingDeliveryMode"); }
}
