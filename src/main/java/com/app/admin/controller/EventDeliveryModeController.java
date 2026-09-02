package com.app.admin.controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.app.admin.model.EventDeliveryMode;
import com.app.admin.service.EventDeliveryModeService;
@RestController @RequestMapping("/api/event-delivery-modes")
public class EventDeliveryModeController extends MaintenanceMasterController<EventDeliveryMode> {
    public EventDeliveryModeController(EventDeliveryModeService service) { super(service); }
}
