package com.app.admin.service;
import org.springframework.stereotype.Service;
import com.app.admin.model.EventDeliveryMode;
import com.app.admin.repository.EventDeliveryModeRepository;
@Service public class EventDeliveryModeService extends MaintenanceMasterService<EventDeliveryMode> {
    public EventDeliveryModeService(EventDeliveryModeRepository repository) { super(repository, "EventDeliveryMode"); }
}
