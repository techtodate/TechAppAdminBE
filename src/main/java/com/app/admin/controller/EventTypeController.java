package com.app.admin.controller;
import org.springframework.web.bind.annotation.*;
import com.app.admin.model.EventType;
import com.app.admin.service.EventTypeService;
@RestController @RequestMapping("/api/event-types")
public class EventTypeController extends MaintenanceMasterController<EventType> { public EventTypeController(EventTypeService service) { super(service); } }
