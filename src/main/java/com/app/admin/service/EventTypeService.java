package com.app.admin.service;
import org.springframework.stereotype.Service;
import com.app.admin.model.EventType;
import com.app.admin.repository.EventTypeRepository;
@Service public class EventTypeService extends MaintenanceMasterService<EventType> {
    public EventTypeService(EventTypeRepository repository) { super(repository, "EventType"); }
}
